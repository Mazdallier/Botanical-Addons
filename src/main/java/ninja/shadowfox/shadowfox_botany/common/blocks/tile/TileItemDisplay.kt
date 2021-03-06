package ninja.shadowfox.shadowfox_botany.common.blocks.tile

import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.ISidedInventory
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.nbt.NBTTagList

class TileItemDisplay() : ShadowFoxTile(), ISidedInventory {
    private val slots = intArrayOf(0)
    private var inventory = arrayOfNulls<ItemStack>(1)

    override fun getSizeInventory(): Int = 1
    override fun getStackInSlot(par1: Int): ItemStack? {
        return this.inventory[par1]
    }

    override fun decrStackSize(par1: Int, par2: Int): ItemStack? {
        if (this.inventory[par1] != null) {
            if (!this.worldObj.isRemote) {
                this.worldObj.markBlockForUpdate(this.xCoord, this.yCoord, this.zCoord)
            }

            val itemstack: ItemStack

            if (inventory[par1]!!.stackSize <= par2) {
                itemstack = inventory[par1]!!
                inventory[par1] = null
                markDirty()
                return itemstack
            } else {
                itemstack = inventory[par1]!!.splitStack(par2)
                if (inventory[par1]!!.stackSize == 0) {
                    inventory[par1] = null
                }

                this.markDirty()
                return itemstack
            }
        } else {
            return null
        }
    }

    override fun getStackInSlotOnClosing(par1: Int): ItemStack? {
        if (inventory[par1] != null) {
            val itemstack = inventory[par1]
            inventory[par1] = null
            return itemstack
        } else {
            return null
        }
    }

    override fun setInventorySlotContents(par1: Int, par2ItemStack: ItemStack?) {
        this.inventory[par1] = par2ItemStack
        if (par2ItemStack != null && par2ItemStack.stackSize > this.inventoryStackLimit) {
            par2ItemStack.stackSize = this.inventoryStackLimit
        }

        this.markDirty()
        if (!this.worldObj.isRemote) {
            this.worldObj.markBlockForUpdate(this.xCoord, this.yCoord, this.zCoord)
        }

    }

    override fun getInventoryName(): String = "container.itemDisplay"
    override fun isUseableByPlayer(p_70300_1_: EntityPlayer?): Boolean = true
    override fun hasCustomInventoryName(): Boolean = false

    override fun readCustomNBT(nbttagcompound: NBTTagCompound) {
        val nbttaglist = nbttagcompound.getTagList("Items", 10)
        this.inventory = arrayOfNulls<ItemStack>(this.sizeInventory)

        for (i in 0..nbttaglist.tagCount() - 1) {
            val nbttagcompound1 = nbttaglist.getCompoundTagAt(i)

            val b0: Int = (nbttagcompound1.getByte("Slot")).toInt()

            if (b0 >= 0 && b0 < inventory.size) {
                this.inventory[b0] = ItemStack.loadItemStackFromNBT(nbttagcompound1)
            }
        }

    }

    override fun writeCustomNBT(nbttagcompound: NBTTagCompound) {
        val nbttaglist = NBTTagList()

        for (i in this.inventory.indices) {
            if (this.inventory[i] != null) {
                val nbttagcompound1 = NBTTagCompound()
                nbttagcompound1.setByte("Slot", i.toByte())
                inventory[i]!!.writeToNBT(nbttagcompound1)
                nbttaglist.appendTag(nbttagcompound1)
            }
        }

        nbttagcompound.setTag("Items", nbttaglist)
    }

    override fun openInventory() {}
    override fun closeInventory() {}

    override fun getInventoryStackLimit(): Int = 1

    override fun canUpdate(): Boolean  = false

    override fun isItemValidForSlot(par1: Int, par2ItemStack: ItemStack): Boolean = true
    override fun getAccessibleSlotsFromSide(par1: Int): IntArray = slots
    override fun canInsertItem(par1: Int, par2ItemStack: ItemStack, par3: Int): Boolean = getStackInSlot(par1) == null
    override fun canExtractItem(par1: Int, par2ItemStack: ItemStack, par3: Int): Boolean = true
}
