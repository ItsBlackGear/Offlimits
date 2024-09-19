package com.blackgear.offlimits.core.registry;

import com.blackgear.platform.common.data.TagRegistry;
import net.minecraft.tags.Tag;
import net.minecraft.world.level.block.Block;

public class OfflimitsBlockTags {
    public static final TagRegistry TAGS = TagRegistry.of("offlimits");
    
    public static final Tag.Named<Block> STONE_ORE_REPLACEABLES = TAGS.blocks("stone_ore_replaceables");
}