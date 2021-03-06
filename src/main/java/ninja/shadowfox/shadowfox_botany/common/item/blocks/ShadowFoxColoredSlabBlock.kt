package ninja.shadowfox.shadowfox_botany.common.item.blocks

import net.minecraft.block.Block
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemSlab
import net.minecraft.item.ItemStack
import net.minecraft.util.StatCollector
import ninja.shadowfox.shadowfox_botany.common.blocks.base.ShadowFoxSlabs
import ninja.shadowfox.shadowfox_botany.common.blocks.colored.BlockColoredWoodSlab


open class ShadowFoxColoredSlabBlock(val par1: Block) : ItemSlab(par1, (par1 as ShadowFoxSlabs).getSingleBlock(), par1.getFullBlock(), false) {

    override fun getUnlocalizedName(par1ItemStack: ItemStack): String {
        return field_150939_a.unlocalizedName.replace("tile.".toRegex(), "tile.shadowfox_botany:").replace("\\d+$".toRegex(), "")
    }

    fun addStringToTooltip(s : String, tooltip : MutableList<Any?>?) {
        tooltip!!.add(s.replace("&".toRegex(), "\u00a7"))
    }

    override fun addInformation(par1ItemStack: ItemStack?, par2EntityPlayer: EntityPlayer?, par3List: MutableList<Any?>?, par4: Boolean) {
        if(par1ItemStack == null) return
        val meta = "\\d+$".toRegex().find(field_150939_a.unlocalizedName)
        addStringToTooltip("&7"+StatCollector.translateToLocal("misc.shadowfox_botany.color." + (meta?.value ?: "16")) + "&r", par3List)
    } 

}
