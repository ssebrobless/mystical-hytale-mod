package com.motm.ui;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

/**
 * UI event payload for the visual spellbook page.
 */
public class SpellbookPageEventData {

    public static final BuilderCodec<SpellbookPageEventData> CODEC =
            BuilderCodec.builder(SpellbookPageEventData.class, SpellbookPageEventData::new)
                    .append(new KeyedCodec<>("Action", Codec.STRING),
                            (data, value) -> data.action = value != null ? value : "",
                            data -> data.action != null ? data.action : "")
                    .add()
                    .append(new KeyedCodec<>("Section", Codec.STRING),
                            (data, value) -> data.section = value != null ? value : "",
                            data -> data.section != null ? data.section : "")
                    .add()
                    .append(new KeyedCodec<>("Value", Codec.STRING),
                            (data, value) -> data.value = value != null ? value : "",
                            data -> data.value != null ? data.value : "")
                    .add()
                    .build();

    public String action;
    public String section;
    public String value;
}
