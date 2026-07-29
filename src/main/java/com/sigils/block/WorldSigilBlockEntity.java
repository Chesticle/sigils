package com.sigils.block;

import com.sigils.item.SigilsItems;
import com.sigils.registry.SigilsComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.Optional;

import com.sigils.cast.CastContext;
import com.sigils.cast.SpellCaster;
import com.sigils.cast.SpellCasting;
import com.sigils.circuit.CircuitCompletion;
import com.sigils.circuit.CircuitSite;
import com.sigils.circuit.Circuits;
import com.sigils.core.draft.InkGrade;
import com.sigils.core.sigil.CircuitLatch;
import com.sigils.core.sigil.SigilIntegrity;
import com.sigils.core.sigil.SigilTint;
import com.sigils.core.spell.CompiledSpell;
import com.sigils.registry.CompiledSpellCodecs;
import com.sigils.registry.SigilsInks;
import com.sigils.registry.SigilsReactions;

/**
 * Everything a drawn sigil remembers: the spell, what drew it, how much of it is
 * left, what closes its ring, and whether that ring was closed a moment ago.
 */
public class WorldSigilBlockEntity extends BlockEntity {

    @Nullable
    private CompiledSpell spell;

    /** Registry id of the ink, or null for a sheet inscribed before Phase 5D. */
    @Nullable
    private String inkGradeId;

    private SigilIntegrity integrity = SigilIntegrity.FULL;
    private Identifier triggerId = Circuits.DEFAULT;
    private final CircuitLatch latch = new CircuitLatch();

    /** Resolved once per trigger change rather than once per tick. */
    @Nullable
    private CircuitCompletion completion;

    public WorldSigilBlockEntity(BlockPos pos, BlockState state) {
        super(SigilsBlocks.WORLD_SIGIL_ENTITY.get(), pos, state);
    }

    // ------------------------------------------------------------------- writing

    /** Called once, by the placement that created this block. */
    public void inscribe(CompiledSpell spell, @Nullable String inkGradeId) {
        this.spell = spell;
        this.inkGradeId = inkGradeId;
        this.integrity = SigilIntegrity.FULL;
        sync();
    }

    // ------------------------------------------------------------------ the loop

    public static void serverTick(net.minecraft.world.level.Level level, BlockPos pos,
                                  BlockState state, WorldSigilBlockEntity sigil) {
        if (level instanceof ServerLevel server) {
            sigil.pollCircuit(server);
        }
    }

    /**
     * The per-tick cost of one placed sigil, in full.
     *
     * <p>Two null checks, an integrity compare and an int compare, and then — for
     * the redstone and closure triggers, which are the ones people actually build
     * with — a return. Nothing scans, nothing allocates, and nothing else in this
     * class runs unless {@link CircuitSite#due} says it's this sigil's turn.
     */
    private void pollCircuit(ServerLevel level) {
        if (spell == null || integrity.inert()) {
            return;
        }
        CircuitCompletion trigger = completion();
        int interval = trigger.pollInterval();
        if (interval <= 0) {
            return; // event-driven — onNeighborChanged wakes it
        }
        CircuitSite site = site(level);
        if (!site.due(interval)) {
            return;
        }
        evaluate(level, site, trigger);
    }

    /** A block update reached us. Only the triggers that asked not to be polled care. */
    void onNeighborChanged(ServerLevel level) {
        if (spell == null || integrity.inert()) {
            return;
        }
        CircuitCompletion trigger = completion();
        if (trigger.pollInterval() > 0) {
            return; // it's on a schedule; a block update is noise to it
        }
        evaluate(level, site(level), trigger);
    }

    private void evaluate(ServerLevel level, CircuitSite site, CircuitCompletion trigger) {
        boolean closed = trigger.isClosed(site);
        if (latch.advance(closed, level.getGameTime(), CircuitLatch.DEFAULT_COOLDOWN_TICKS)) {
            fire(level);
        }
        setChanged(); // the latch moved; it has to survive an unload
    }

    private void fire(ServerLevel level) {
        // Explicit centre rather than BlockPos.getCenter(), which 26.x removed.
        Vec3 origin = new Vec3(
                worldPosition.getX() + 0.5,
                worldPosition.getY() + 0.5,
                worldPosition.getZ() + 0.5);

        // Null caster: nobody is holding this. Targeting.resolve falls back to the
        // origin on every branch, which is the sigil's own position.
        Optional<CastContext> context = SpellCasting.begin(level, null, origin);
        if (context.isEmpty()) {
            return; // this tick's global spell budget is spent
        }

        SpellCaster.cast(
                context.get(),
                spell,
                SigilsReactions.load(level.registryAccess()),
                integrity.instabilityFactor());

        level.playSound(null, worldPosition, SoundEvents.ENCHANTMENT_TABLE_USE,
                SoundSource.BLOCKS, 0.45f, 1.4f);

        flash(level);
    }

    /**
     * Light the mark for a moment.
     *
     * <p>A scheduled tick puts it out rather than a countdown field, for the same
     * reason {@link CircuitLatch} stores a deadline: the block does nothing at all
     * while it waits, and an unloaded chunk resumes correctly on its own.
     */
    private void flash(ServerLevel level) {
        BlockState state = getBlockState();
        if (!state.getValue(WorldSigilBlock.LIT)) {
            level.setBlock(worldPosition, state.setValue(WorldSigilBlock.LIT, true),
                    Block.UPDATE_CLIENTS);
        }
        level.scheduleTick(worldPosition, state.getBlock(), WorldSigilBlock.FLASH_TICKS);
    }

    private CircuitSite site(ServerLevel level) {
        return new CircuitSite(level, worldPosition, getBlockState().getValue(WorldSigilBlock.FACING));
    }

    private CircuitCompletion completion() {
        if (completion == null) {
            completion = Circuits.get(triggerId);
        }
        return completion;
    }

    // -------------------------------------------------------------- interaction

    /**
     * Advance to the next registered trigger.
     *
     * <p>The latch is re-seeded with whatever the new trigger reads <em>right
     * now</em>, so switching to Tread while standing on the sigil doesn't count as
     * a rising edge and fire it under your feet. Step off and back on and it works
     * normally.
     */
    public Identifier cycleTrigger(ServerLevel level) {
        triggerId = Circuits.next(triggerId);
        completion = null;

        boolean closedNow = completion().isClosed(site(level));
        latch.restore(closedNow, latch.readyAt());

        sync();
        return triggerId;
    }

    // ------------------------------------------------------------- presentation

    /**
     * The ink's raw RGB, or {@code -1} if it can't be determined.
     *
     * <p>This is now the <em>only</em> thing the renderer asks a sigil for, and it
     * is written once at placement and never changed. Wear and the flash both live
     * in the block state, where the renderer was already watching. A tint that
     * depends on nothing mutable in a block entity cannot render stale.
     */
    public int inkTint() {
        if (level == null || inkGradeId == null) {
            return -1;
        }
        return SigilsInks.byId(level.registryAccess(), inkGradeId)
                .map(InkGrade::tint)
                .orElse(-1);
    }

    /** A sheet carrying this sigil's spell, for peeling it back off the wall. */
    public ItemStack recoverSheet() {
        ItemStack sheet = new ItemStack(SigilsItems.PARCHMENT.get());
        if (spell != null) {
            sheet.set(SigilsComponents.SPELL.get(), spell);
        }
        if (inkGradeId != null) {
            sheet.set(SigilsComponents.INK_GRADE.get(), inkGradeId);
        }
        return sheet;
    }

    @Nullable
    public CompiledSpell spell() {
        return spell;
    }

    @Nullable
    public String inkGradeId() {
        return inkGradeId;
    }

    public SigilIntegrity integrity() {
        return integrity;
    }

    /** Part C sets this. Part B only ever reads it. */
    /**
     * Wear the sigil.
     *
     * <p>Called from three places that all have very different rates: a bucket
     * (once), a sponge (once) and rain (repeatedly, for minutes). So the packet is
     * spent only when something visible changed — a wear <em>step</em>, not a wear
     * <em>value</em>. The float is still saved every time, because it's the real
     * quantity and the block state is only a picture of it.
     */
    public void setIntegrity(SigilIntegrity updated) {
        if (level == null || level.isClientSide()) {
            return;
        }
        int before = integrity.wearStep();
        integrity = updated;
        setChanged(); // always: it has to survive a save, packet or no packet

        if (before == updated.wearStep()) {
            return; // nothing anyone could see changed
        }
        // Read the state back rather than trusting the cached one: the flash may
        // have changed LIT since this block entity last looked.
        BlockState current = level.getBlockState(worldPosition);
        level.setBlock(worldPosition,
                current.setValue(WorldSigilBlock.WEAR, updated.wearStep()),
                Block.UPDATE_CLIENTS);
    }

    public Identifier triggerId() {
        return triggerId;
    }

    // -------------------------------------------------------------------- index

    @Override
    public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel server) {
            SigilIndex.of(server).add(worldPosition);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level instanceof ServerLevel server) {
            SigilIndex.of(server).remove(worldPosition);
        }
    }

    // -------------------------------------------------------------- persistence

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (spell != null) {
            output.store("spell", CompiledSpellCodecs.CODEC, spell);
        }
        if (inkGradeId != null) {
            output.putString("ink_grade", inkGradeId);
        }
        output.putFloat("integrity", integrity.value());
        output.putString("trigger", triggerId.toString());
        output.putBoolean("closed", latch.closed());
        output.putLong("ready_at", latch.readyAt());
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        spell = input.read("spell", CompiledSpellCodecs.CODEC).orElse(null);
        inkGradeId = input.getString("ink_grade").orElse(null);
        integrity = new SigilIntegrity(input.getFloatOr("integrity", 1f));

        String trigger = input.getString("trigger").orElse(Circuits.DEFAULT.toString());
        Identifier parsed = Identifier.tryParse(trigger);
        triggerId = parsed == null ? Circuits.DEFAULT : parsed;
        completion = null;

        latch.restore(input.getBooleanOr("closed", false), input.getLongOr("ready_at", 0L));
    }

    // ------------------------------------------------------------- client sync

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    private void sync() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(),
                    Block.UPDATE_CLIENTS);
        }
    }
}