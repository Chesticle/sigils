package com.sigils.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import com.sigils.circuit.CircuitSite;
import com.sigils.circuit.Circuits;
import com.sigils.core.draft.InkGrade;
import com.sigils.core.draft.PenCapabilities;
import com.sigils.core.sigil.CircuitLatch;
import com.sigils.core.sigil.PressReadiness;
import com.sigils.core.sigil.PressRules;
import com.sigils.core.spell.CompiledSpell;
import com.sigils.draft.InkSupply;
import com.sigils.draft.ParchmentGrades;
import com.sigils.registry.SigilsComponents;
import com.sigils.registry.SigilsPens;

/**
 * Three slots and a latch.
 *
 * <p>Implements {@link Container} and nothing else, which is enough for hoppers:
 * ink goes in from any side, so a press feeding itself from a chest is four
 * blocks of build rather than a feature anyone had to write.
 */
public class SpellPressBlockEntity extends BlockEntity implements Container {

    public static final int SLOT_TEMPLATE = 0;
    public static final int SLOT_PEN = 1;
    public static final int SLOT_INK = 2;
    public static final int SLOT_COUNT = 3;

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private final CircuitLatch latch = new CircuitLatch();

    public SpellPressBlockEntity(BlockPos pos, BlockState state) {
        super(SigilsBlocks.SPELL_PRESS_ENTITY.get(), pos, state);
    }

    // ------------------------------------------------------------------- the loop

    /**
     * A signal changed nearby.
     *
     * <p>No ticker: the press's trigger is the same {@code sigils:redstone}
     * instance a sigil uses, and that trigger declares {@code pollInterval() == 0}
     * — it is woken by block updates and asks nothing of the server in between.
     */
    void onNeighborChanged(ServerLevel level) {
        CircuitSite site = new CircuitSite(level, worldPosition, facing());
        boolean closed = Circuits.get(Circuits.REDSTONE).isClosed(site);

        if (latch.advance(closed, level.getGameTime(), PressRules.COOLDOWN_TICKS)) {
            stamp(level);
        }
        setChanged();
    }

    private void stamp(ServerLevel level) {
        if (!readiness().ready()) {
            level.playSound(null, worldPosition, SoundEvents.DISPENSER_FAIL,
                    SoundSource.BLOCKS, 0.7f, 1f);
            return;
        }

        RegistryAccess registries = level.registryAccess();
        CompiledSpell spell = items.get(SLOT_TEMPLATE).get(SigilsComponents.SPELL.get());
        InkGrade grade = InkSupply.gradeOf(registries, items.get(SLOT_INK)).orElse(null);
        if (spell == null || grade == null) {
            return; // readiness() already said otherwise, but never trust two sources
        }

        // The grade recorded on the copy is the press's ink, not the master's — so
        // a magical-ink template loaded into a press full of netherite ink prints
        // rain-proof sigils. That falls out of storing a grade rather than a flag.
        if (!WorldSigilPlacement.stamp(level, target(), facing(), spell, grade.id())) {
            return;
        }
        if (!inkless(registries)) {
            InkSupply.spend(registries, items.get(SLOT_INK), PressRules.INK_PER_STAMP);
        }

        level.playSound(null, worldPosition, SoundEvents.PISTON_EXTEND,
                SoundSource.BLOCKS, 0.6f, 1.4f);
        setChanged();
    }

    // -------------------------------------------------------------------- status

    public PressReadiness readiness() {
        if (level == null) {
            return PressReadiness.NO_TEMPLATE;
        }
        RegistryAccess registries = level.registryAccess();

        return PressRules.evaluate(
                items.get(SLOT_TEMPLATE).has(SigilsComponents.SPELL.get()),
                !items.get(SLOT_PEN).isEmpty(),
                inkless(registries),
                InkSupply.capacityOf(registries, items.get(SLOT_INK)),
                WorldSigilPlacement.canStamp(level, target(), facing()),
                level instanceof ServerLevel server
                        ? SigilIndex.of(server).within(target(), PressRules.NEARBY_RADIUS).size()
                        : 0);
    }

    private boolean inkless(RegistryAccess registries) {
        PenCapabilities pen = SigilsPens.table(registries).get(items.get(SLOT_PEN).getItem());
        return pen != null && pen.inklessOnSolids();
    }

    /** Ink units remaining, as a string, for the status message. */
    public String inkSummary() {
        if (level == null) {
            return "0";
        }
        return String.format("%.0f",
                InkSupply.capacityOf(level.registryAccess(), items.get(SLOT_INK)));
    }

    /** 0 when it can't print; otherwise roughly how many stamps are left, capped at 15. */
    public int comparatorOutput() {
        if (level == null || !readiness().ready()) {
            return 0;
        }
        if (inkless(level.registryAccess())) {
            return 15;
        }
        float units = InkSupply.capacityOf(level.registryAccess(), items.get(SLOT_INK));
        return Math.clamp(1 + (int) (units / PressRules.INK_PER_STAMP), 1, 15);
    }

    private Direction facing() {
        return getBlockState().getValue(SpellPressBlock.FACING);
    }

    private BlockPos target() {
        return worldPosition.relative(facing());
    }

    // ------------------------------------------------------------------- loading

    /**
     * Put a held item into whichever slot accepts it, handing back whatever was
     * there. Returns false if nothing here wants it.
     */
    public boolean load(ItemStack held, Player player, InteractionHand hand) {
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            if (!canPlaceItem(slot, held)) {
                continue;
            }
            ItemStack previous = items.get(slot);
            items.set(slot, held.copyWithCount(slot == SLOT_INK ? held.getCount() : 1));
            player.setItemInHand(hand, previous);
            setChanged();
            return true;
        }
        return false;
    }

    // ----------------------------------------------------------------- Container

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (stack.isEmpty() || level == null) {
            return false;
        }
        RegistryAccess registries = level.registryAccess();
        return switch (slot) {
            case SLOT_TEMPLATE -> ParchmentGrades.isParchment(registries, stack)
                    && stack.has(SigilsComponents.SPELL.get());
            case SLOT_PEN -> SigilsPens.table(registries).containsKey(stack.getItem());
            case SLOT_INK -> InkSupply.isInk(registries, stack);
            default -> false;
        };
    }

    @Override
    public int getContainerSize() {
        return SLOT_COUNT;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack removed = ContainerHelper.removeItem(items, slot, amount);
        if (!removed.isEmpty()) {
            setChanged();
        }
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        stack.limitSize(getMaxStackSize(stack));
        setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        items.clear();
        setChanged();
    }

    // --------------------------------------------------------------- persistence

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
        output.putBoolean("closed", latch.closed());
        output.putLong("ready_at", latch.readyAt());
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        items.clear();
        ContainerHelper.loadAllItems(input, items);
        latch.restore(input.getBooleanOr("closed", false), input.getLongOr("ready_at", 0L));
    }
}