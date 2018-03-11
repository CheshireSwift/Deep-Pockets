package com.rebelkeithy.deeppockets;

import com.google.common.collect.Lists;
import com.rebelkeithy.deeppockets.item.ItemMiningPack.StoredOre;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.oredict.OreDictionary;

import java.util.Collections;
import java.util.List;

@Config(modid = DeepPockets.MODID)
@Mod.EventBusSubscriber(modid = DeepPockets.MODID)
public class DeepPocketsConfig {

    @Config.Comment({ "Items other than ore blocks" })
    public static String[] allowedItems = new String[] {
            "minecraft:coal",
            "minecraft:redstone",
            "minecraft:dye:4",
            "minecraft:diamond",
            "minecraft:emerald",
            "minecraft:quartz"
    };
    @Config.Ignore
    public static List<String> builtItems = Lists.newArrayList();

    @SubscribeEvent
    public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
        if (event.getModID().equals(DeepPockets.MODID)) {
            ConfigManager.sync(DeepPockets.MODID, Config.Type.INSTANCE);
            buildItemList();
        }
    }

    public static void buildItemList() {
        builtItems.clear();
        Collections.addAll(builtItems, allowedItems);
    }

    // Returns null if not an ore, otherwise returns the oredict name
    public static String isOre(ItemStack stack) {
        if (builtItems.contains(stack.getItem().getRegistryName().toString()))
            return stack.getItem().getRegistryName().toString();

        if (builtItems.contains(stack.getItem().getRegistryName().toString() + ":" + stack.getMetadata()))
            return stack.getItem().getRegistryName().toString() + ":" + stack.getMetadata();

        int[] ids = OreDictionary.getOreIDs(stack);
        if (ids.length > 0) {
            String name = OreDictionary.getOreName(ids[0]);
            return name.contains("ore") ? name : null;
        }

        return null;
    }

    public static ItemStack getStack(StoredOre ore, int amount) {
        Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(ore.registryName));
        return new ItemStack(item, amount, ore.meta);
    }
}
