package com.rebelkeithy.deeppockets.item;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.rebelkeithy.deeppockets.DeepPocketsConfig;
import com.rebelkeithy.deeppockets.DeepPockets;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.oredict.OreDictionary;

import javax.annotation.Nullable;
import java.util.*;

public class ItemMiningPack extends Item {

    public static final TreeMap<Integer, String> PREFIX = Maps.newTreeMap();

    static {
        PREFIX.put(0, "no");
        PREFIX.put(1, "few");
        PREFIX.put(5, "several");
        PREFIX.put(16, "piles");
        PREFIX.put(32, "lots");
        PREFIX.put(64, "hoards");
        PREFIX.put(128, "throngs");
        PREFIX.put(256, "swarms");
        PREFIX.put(512, "zounds");
        PREFIX.put(1024, "legions");
    }

    private final PackTypes type;

    public ItemMiningPack(PackTypes type) {
        this.type = type;

        setUnlocalizedName(DeepPockets.MODID + ".mining_pack_" + type.getName());
        setMaxStackSize(1);
        setCreativeTab(CreativeTabs.TOOLS);
    }

    public PackTypes getType() {
        return type;
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null)
            return;

        NBTTagCompound ores = tag.getCompoundTag("ores");

        if (ores.getKeySet().size() > 0)
            tooltip.add(TextFormatting.GRAY + I18n.format("tooltip.deeppockets.slots_used", ores.getSize(), type.getSlots()));

        for (String ore : ores.getKeySet()) {
            int amount = ores.getCompoundTag(ore).getInteger("amount");
            String name = ores.getCompoundTag(ore).getString("name");
            String prefix = "prefix.deeppockets." + PREFIX.ceilingEntry(amount).getValue();

            String lock = "";
            if (ores.getCompoundTag(ore).getBoolean("lock"))
                lock = TextFormatting.DARK_GRAY + " *";

            tooltip.add(TextFormatting.GRAY + I18n.format(prefix, TextFormatting.DARK_AQUA + name.replace(" Ore", "") + lock)); // FIXME This Ore replacement will not work in other langs
        }
    }

    /**
     * Called when a Block is right-clicked with this Item
     */
    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World worldIn, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        TileEntity te = worldIn.getTileEntity(pos);
        if (te != null && te.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, facing)) {
            IItemHandler itemHandler = te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, facing);
            if (itemHandler instanceof IItemHandlerModifiable) {

                ItemStack itemStack = player.getHeldItem(hand);

                if (worldIn.isRemote) {
                    if (!isEmpty(itemStack))
                        worldIn.playSound(player, pos, SoundEvents.BLOCK_GRAVEL_FALL, SoundCategory.PLAYERS, 0.5f, 0.6f);
                    return EnumActionResult.PASS;
                }
                return this.depositOresRandomly((IItemHandlerModifiable) itemHandler, itemStack);
            }
        }


        IBlockState block = worldIn.getBlockState(pos);
        ItemStack clickedBlockStack = block.getBlock().getPickBlock(block, null, worldIn, pos, player);
        if (block.getBlock() == Blocks.LIT_REDSTONE_ORE)
            clickedBlockStack = new ItemStack(Blocks.REDSTONE_ORE, 1);

        if (clickedBlockStack.isEmpty())
            return EnumActionResult.PASS;

        int[] oreIDs = OreDictionary.getOreIDs(clickedBlockStack);
        if (oreIDs.length > 0) {
            String oreName = OreDictionary.getOreName(oreIDs[0]);
            if (oreName.contains("ore")) {
                ItemStack itemStack = player.getHeldItemMainhand();
                NBTTagCompound tag = itemStack.getTagCompound();
                if (tag == null)
                    tag = new NBTTagCompound();

                NBTTagCompound ores = tag.getCompoundTag("ores");
                NBTTagCompound thisOre = ores.getCompoundTag(oreName);
                if (thisOre.getBoolean("lock")) {
                    thisOre.setBoolean("lock", false);
                    worldIn.playSound(player, pos, SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 0.3f, 0.5f);
                } else if (ores.getSize() < type.getSlots()) {
                    thisOre.setBoolean("lock", true);
                    thisOre.setString("name", clickedBlockStack.getDisplayName());
                    thisOre.setString("registry", clickedBlockStack.getItem().getRegistryName().toString());
                    thisOre.setInteger("meta", clickedBlockStack.getItemDamage());
                    if (!thisOre.hasKey("amount"))
                        thisOre.setInteger("amount", 0);
                    worldIn.playSound(player, pos, SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 0.3f, 0.8f);
                }


                ores.setTag(oreName, thisOre);
                if (thisOre.getInteger("amount") == 0 && !thisOre.getBoolean("lock")) {
                    ores.removeTag(oreName);
                }
                tag.setTag("ores", ores);
                itemStack.setTagCompound(tag);
            }
        }

        return EnumActionResult.PASS;
    }

    public EnumActionResult depositOresRandomly(IItemHandlerModifiable inventory, ItemStack itemStack) {
        NBTTagCompound tag = itemStack.getTagCompound();
        if (tag == null)
            return EnumActionResult.PASS;

        Random rand = new Random();
        List<StoredOre> ores = Lists.newArrayList();

        NBTTagCompound oresTag = tag.getCompoundTag("ores");
        for (String ore : oresTag.getKeySet()) {
            ores.add(this.getOre(itemStack, ore));
        }

        if (ores.isEmpty())
            return EnumActionResult.PASS;

        boolean done = false;
        while (!done) {
            StoredOre ore = ores.get(rand.nextInt(ores.size()));
            int amount = Math.min(ore.amount, rand.nextInt(45));

            ItemStack stack = DeepPocketsConfig.getStack(ore, amount);

            List<Integer> possibleSlots = Lists.newArrayList();
            for (int i = 0; i < inventory.getSlots(); i++) {
                ItemStack returned = inventory.insertItem(i, stack, true);
                if (returned != stack) {
                    if (inventory.getStackInSlot(i).isEmpty()) {
                        possibleSlots.add(i);
                    } else if (ItemHandlerHelper.canItemStacksStack(inventory.getStackInSlot(i), stack)) {
                        ItemStack slotStack = inventory.getStackInSlot(i);
                        if (slotStack.getCount() < slotStack.getMaxStackSize())
                            possibleSlots.add(i);
                    }
                }
            }

            if (possibleSlots.isEmpty()) {
                ores.remove(ore);
                this.setOre(itemStack, ore.oreDictName, ore);

                if (ores.isEmpty())
                    done = true;

                continue;
            }

            int index = possibleSlots.get(rand.nextInt(possibleSlots.size()));
            ItemStack slotStack = inventory.getStackInSlot(index);


            if (slotStack.isEmpty()) {
                inventory.setStackInSlot(index, stack.copy());
                ore.amount -= amount;
            } else if (ItemHandlerHelper.canItemStacksStack(slotStack, stack)) {
                int amountToTransfer = Math.min(amount, 64 - slotStack.getCount());
                ItemStack toTransfer = stack.splitStack(amountToTransfer);

                int fullAmount = toTransfer.getCount() + slotStack.getCount();
                inventory.getStackInSlot(index).setCount(fullAmount); // TODO - Change this. It breaks contract
                ore.amount -= toTransfer.getCount();
            }

            if (ore.amount == 0) {
                ores.remove(ore);
                this.setOre(itemStack, ore.oreDictName, ore);

                if (ores.isEmpty())
                    done = true;
            }

        }


        return EnumActionResult.PASS;
    }

    public boolean containsOre(ItemStack pack, String oreName) {
        if (pack.getTagCompound() == null)
            return false;

        return pack.getTagCompound().getCompoundTag("ores").hasKey(oreName);
    }

    public int getOreAmount(ItemStack pack, String oreName) {
        if (pack.getTagCompound() == null || !containsOre(pack, oreName))
            return 0;

        return pack.getTagCompound().getCompoundTag("ores").getCompoundTag(oreName).getInteger("amount");
    }

    public int numStoredOres(ItemStack pack) {
        if (pack.getTagCompound() == null)
            return 0;

        return pack.getTagCompound().getCompoundTag("ores").getSize();
    }

    public StoredOre getOre(ItemStack pack, String oreName) {
        if (pack.getTagCompound() == null || !pack.getTagCompound().getCompoundTag("ores").hasKey(oreName)) {
            return new StoredOre(oreName, 0, "");
        }

        return new StoredOre(pack.getTagCompound().getCompoundTag("ores").getCompoundTag(oreName), oreName);
    }

    public void setOre(ItemStack pack, String oreName, StoredOre ore) {
        if (pack.getTagCompound() == null)
            pack.setTagCompound(new NBTTagCompound());

        NBTTagCompound oreTag = pack.getTagCompound().getCompoundTag("ores");
        ore.save(oreTag);
        pack.getTagCompound().setTag("ores", oreTag);
    }

    public boolean isEmpty(ItemStack pack) {
        NBTTagCompound tag = pack.getTagCompound();
        if (tag == null)
            return true;

        NBTTagCompound oresTag = tag.getCompoundTag("ores");
        for (String ore : oresTag.getKeySet()) {
            if (tag.getCompoundTag("ores").getCompoundTag(ore).getInteger("amount") > 0)
                return false;
        }

        return true;
    }

    public int getTotalItems(ItemStack pack) {
        NBTTagCompound tag = pack.getTagCompound();
        if (tag == null)
            return 0;

        int items = 0;
        NBTTagCompound oresTag = tag.getCompoundTag("ores");
        for (String ore : oresTag.getKeySet()) {
            items += tag.getCompoundTag("ores").getCompoundTag(ore).getInteger("amount");
        }

        return items;
    }

    public class StoredOre {
        public String oreDictName;
        public String registryName;
        public int meta;
        public int amount;
        public String displayName;
        public boolean locked;

        public StoredOre(String oreDictName, int amount, String displayName, boolean locked) {
            this.oreDictName = oreDictName;
            this.amount = amount;
            this.displayName = displayName;
            this.locked = locked;
        }

        public StoredOre(String oreDictName, int amount, String displayName) {
            this(oreDictName, amount, displayName, false);
        }

        public StoredOre(NBTTagCompound tag, String oreDictName) {
            this.oreDictName = oreDictName;
            this.registryName = tag.getString("registry");
            this.meta = tag.getInteger("meta");
            this.amount = tag.getInteger("amount");
            this.displayName = tag.getString("name");
            this.locked = tag.getBoolean("lock");
        }

        public void save(NBTTagCompound tag) {
            if (amount > 0 || locked) {
                NBTTagCompound oreTag = new NBTTagCompound();
                oreTag.setString("registry", registryName);
                oreTag.setInteger("meta", meta);
                oreTag.setInteger("amount", amount);
                oreTag.setString("name", displayName);
                oreTag.setBoolean("lock", locked);
                tag.setTag(oreDictName, oreTag);
            } else {
                tag.removeTag(oreDictName);
            }
        }
    }
}
