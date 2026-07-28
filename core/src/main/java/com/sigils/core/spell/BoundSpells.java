package com.sigils.core.spell;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The spells bound into a sketchbook, and which one is selected.
 *
 * <p>Immutable, like everything else that ends up in a data component: every
 * mutation returns a new instance, which is what makes the component's
 * {@code equals} meaningful and stops two stacks sharing a mutable list.
 *
 * @param spells the bound spells, in binding order
 * @param active index into {@code spells}, always in range when non-empty
 */
public record BoundSpells(List<CompiledSpell> spells, int active) {

    /**
     * How many spells one book holds.
     *
     * <p>Also the network cap — the stream codec refuses a longer list, so a
     * hand-written packet can't make the server allocate an unbounded one.
     */
    public static final int MAX_PAGES = 8;

    public static final BoundSpells EMPTY = new BoundSpells(List.of(), 0);

    public BoundSpells {
        spells = List.copyOf(spells);
        // Clamp rather than throw: this record is built from a data component
        // that may have been written by an older version, or edited by hand.
        active = spells.isEmpty() ? 0 : Math.clamp(active, 0, spells.size() - 1);
    }

    public boolean isEmpty() {
        return spells.isEmpty();
    }

    public boolean full() {
        return spells.size() >= MAX_PAGES;
    }

    public int size() {
        return spells.size();
    }

    /** The spell that would be cast right now. */
    public Optional<CompiledSpell> activeSpell() {
        return spells.isEmpty() ? Optional.empty() : Optional.of(spells.get(active));
    }

    /**
     * Add a spell and select it. Returns {@code this} unchanged if the book is
     * already full — the caller checks {@link #full()} to explain why.
     */
    public BoundSpells bind(CompiledSpell spell) {
        if (full()) {
            return this;
        }
        List<CompiledSpell> updated = new ArrayList<>(spells);
        updated.add(spell);
        return new BoundSpells(updated, updated.size() - 1);
    }

    /** Move the selection, wrapping at both ends. Direction is a sign, not a count. */
    public BoundSpells cycled(int direction) {
        if (spells.size() < 2 || direction == 0) {
            return this;
        }
        int step = direction > 0 ? 1 : -1;
        int next = Math.floorMod(active + step, spells.size());
        return new BoundSpells(spells, next);
    }

    /** Remove a page, keeping the selection as close to where it was as possible. */
    public BoundSpells without(int index) {
        if (index < 0 || index >= spells.size()) {
            return this;
        }
        List<CompiledSpell> updated = new ArrayList<>(spells);
        updated.remove(index);
        return new BoundSpells(updated, index <= active ? active - 1 : active);
    }
}