package com.sigils.core.glyph;

/**
 * What a glyph does in a spell.
 *
 * <ul>
 *   <li>{@code CREST}    — contributes elemental content (the "what")</li>
 *   <li>{@code MODIFIER} — shapes delivery: form, size, target (the "how")</li>
 *   <li>{@code RING}     — the enclosure that makes the circle castable</li>
 *   <li>{@code LINK}     — connects crests into compound spells (later phases)</li>
 * </ul>
 */
public enum GlyphRole {
    CREST,
    MODIFIER,
    RING,
    LINK
}