package com.buuz135.functionalstorage.block.tile;

import com.buuz135.functionalstorage.item.LinkingToolItem;
import com.buuz135.functionalstorage.item.StorageUpgradeItem;
import com.buuz135.functionalstorage.item.UpgradeItem;
import com.hrznstudio.titanium.annotation.Save;
import com.hrznstudio.titanium.block.BasicTileBlock;
import com.hrznstudio.titanium.block.tile.ActiveTile;
import com.hrznstudio.titanium.client.screen.addon.TextScreenAddon;
import com.hrznstudio.titanium.component.inventory.InventoryComponent;
import com.hrznstudio.titanium.util.RayTraceUtils;
import com.hrznstudio.titanium.util.TileUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.UUID;

public abstract class ControllableDrawerTile<T extends ControllableDrawerTile<T>> extends ActiveTile<T> {

    private static HashMap<UUID, Long> INTERACTION_LOGGER = new HashMap<>();

    @Save
    private BlockPos controllerPos;
    @Save
    private InventoryComponent<ControllableDrawerTile<T>> storageUpgrades;
    @Save
    private InventoryComponent<ControllableDrawerTile<T>> utilityUpgrades;

    public ControllableDrawerTile(BasicTileBlock<T> base, BlockPos pos, BlockState state) {
        super(base, pos, state);
        this.addInventory((InventoryComponent<T>) (this.storageUpgrades = new InventoryComponent<ControllableDrawerTile<T>>("storage_upgrades", 10, 70, getStorageSlotAmount())
                        .setInputFilter((stack, integer) -> stack.getItem() instanceof UpgradeItem && ((UpgradeItem) stack.getItem()).getType() == UpgradeItem.Type.STORAGE))
        );
        this.addInventory((InventoryComponent<T>) (this.utilityUpgrades = new InventoryComponent<ControllableDrawerTile<T>>("utility_upgrades", 114, 70, 3)
                .setInputFilter((stack, integer) -> stack.getItem() instanceof UpgradeItem && ((UpgradeItem) stack.getItem()).getType() == UpgradeItem.Type.UTILITY))
        );
        addGuiAddonFactory(() -> new TextScreenAddon("Storage", 10, 59, false, ChatFormatting.DARK_GRAY.getColor()));
        addGuiAddonFactory(() -> new TextScreenAddon("Utility", 114, 59, false, ChatFormatting.DARK_GRAY.getColor()));
        addGuiAddonFactory(() -> new TextScreenAddon("key.categories.inventory", 8, 92, false, ChatFormatting.DARK_GRAY.getColor()){
            @Override
            public String getText() {
                return  new TranslatableComponent("key.categories.inventory").getString();
            }
        });
    }

    public BlockPos getControllerPos() {
        return controllerPos;
    }

    public void setControllerPos(BlockPos controllerPos) {
        if (this.controllerPos != null){
            TileUtil.getTileEntity(getLevel(), this.controllerPos, DrawerControllerTile.class).ifPresent(drawerControllerTile -> {
                drawerControllerTile.addConnectedDrawers(LinkingToolItem.ActionMode.REMOVE, getBlockPos());
            });
        }
        this.controllerPos = controllerPos;
    }

    public int getStorageMultiplier(){
        int mult = 1;
        for (int i = 0; i < storageUpgrades.getSlots(); i++) {
            if (storageUpgrades.getStackInSlot(i).getItem() instanceof StorageUpgradeItem){
                if (mult == 1) mult = ((StorageUpgradeItem) storageUpgrades.getStackInSlot(i).getItem()).getStorageMultiplier();
                else mult *= ((StorageUpgradeItem) storageUpgrades.getStackInSlot(i).getItem()).getStorageMultiplier();
            }
        }
        return mult;
    }

    public InteractionResult onSlotActivated(Player playerIn, InteractionHand hand, Direction facing, double hitX, double hitY, double hitZ, int slot) {
        if (super.onActivated(playerIn, hand, facing, hitX, hitY, hitZ) == InteractionResult.SUCCESS) {
            return InteractionResult.SUCCESS;
        }
        if (slot == -1){
            openGui(playerIn);
        } else if (isServer()){
            ItemStack stack = playerIn.getItemInHand(hand);
            if (!stack.isEmpty() && getStorage().isItemValid(slot, stack)) {
                playerIn.setItemInHand(hand, getStorage().insertItem(slot, stack, false));
            } else if (System.currentTimeMillis() - INTERACTION_LOGGER.getOrDefault(playerIn.getUUID(), System.currentTimeMillis()) < 300) {
                for (ItemStack itemStack : playerIn.getInventory().items) {
                    if (!itemStack.isEmpty() && getStorage().insertItem(slot, itemStack, true).isEmpty()) {
                        getStorage().insertItem(slot, itemStack.copy(), false);
                        itemStack.setCount(0);
                    }
                }
            }
            INTERACTION_LOGGER.put(playerIn.getUUID(), System.currentTimeMillis());
        }
        return InteractionResult.SUCCESS;
    }

    public abstract int getStorageSlotAmount();

    public void onClicked(Player playerIn, int slot) {
        if (isServer() && slot != -1){
            HitResult rayTraceResult = RayTraceUtils.rayTraceSimple(this.level, playerIn, 16, 0);
            if (rayTraceResult.getType() == HitResult.Type.BLOCK) {
                BlockHitResult blockResult = (BlockHitResult) rayTraceResult;
                Direction facing = blockResult.getDirection();
                if (facing.equals(this.getFacingDirection())){
                    ItemHandlerHelper.giveItemToPlayer(playerIn, getStorage().extractItem(slot, playerIn.isShiftKeyDown() ? 64 : 1, false));
                }
            }
        }
    }


    public abstract IItemHandler getStorage();

    public abstract LazyOptional<IItemHandler> getOptional();

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        getOptional().invalidate();
    }
}