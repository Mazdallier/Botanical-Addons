package ninja.shadowfox.shadowfox_botany.common.item.rods

import cpw.mods.fml.relauncher.Side
import cpw.mods.fml.relauncher.SideOnly
import cpw.mods.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.client.event.TextureStitchEvent
import net.minecraftforge.common.MinecraftForge
import net.minecraft.command.IEntitySelector
import net.minecraft.client.renderer.texture.IIconRegister
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityCreature
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.effect.EntityLightningBolt
import net.minecraft.entity.monster.EntitySlime
import net.minecraft.entity.monster.IMob
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.item.EnumAction
import net.minecraft.item.ItemStack
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.IIcon
import net.minecraft.util.MathHelper
import net.minecraft.util.AxisAlignedBB
import net.minecraft.util.DamageSource
import net.minecraft.util.ResourceLocation
import net.minecraft.world.World
import ninja.shadowfox.shadowfox_botany.common.core.handler.ConfigHandler
import ninja.shadowfox.shadowfox_botany.common.item.IPriestColorOverride
import ninja.shadowfox.shadowfox_botany.common.item.StandardItem
import ninja.shadowfox.shadowfox_botany.common.item.baubles.ItemPriestEmblem
import vazkii.botania.api.item.IAvatarTile
import vazkii.botania.api.item.IAvatarWieldable
import vazkii.botania.api.item.IManaProficiencyArmor
import vazkii.botania.api.mana.IManaUsingItem
import vazkii.botania.api.mana.ManaItemHandler
import vazkii.botania.common.Botania
import vazkii.botania.common.core.helper.ItemNBTHelper
import vazkii.botania.common.core.helper.Vector3
import vazkii.botania.common.entity.EntityDoppleganger
import vazkii.botania.client.render.block.InterpolatedIcon
import java.awt.Color
import java.util.*
import kotlin.properties.Delegates

public open class LightningRod(name: String = "lightningRod") : StandardItem(name), IManaUsingItem, IAvatarWieldable {
    private val avatarOverlay = ResourceLocation("shadowfox_botany:textures/model/avatarLightning.png")
    private val COST_AVATAR = 150

    var icon: IIcon by Delegates.notNull()

    val COST = 300
    val PRIEST_COST = 200
    val THOR_COST = 700
    val PROWESS_COST = 50

    val SPEED = 90
    val PRIEST_SPEEDUP = 30
    val THOR_SPEEDUP = 10
    val PROWESS_SPEEDUP = 10

    val DAMAGE = 8f
    val PRIEST_POWERUP = 3f
    val THOR_POWERUP = 7f
    val PROWESS_POWERUP = 2f

    val CHAINRANGE = 7f
    val PRIEST_RANGEUP = -1f
    val THOR_RANGEUP = 1f
    val PROWESS_RANGEUP = 1f

    val TARGETS = 4
    val PRIEST_TARGETS = 2
    val THOR_TARGETS = -2
    val PROWESS_TARGETS = 1


    init {
        setMaxStackSize(1)
        MinecraftForge.EVENT_BUS.register(this)
    }

    @SideOnly(Side.CLIENT)
    override fun registerIcons(par1IconRegister: IIconRegister) {}

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    fun loadTextures(event: TextureStitchEvent.Pre) {
        if(event.map.textureType == 1) {
            var localIcon = InterpolatedIcon("shadowfox_botany:lightningRod")
            if(event.map.setTextureEntry("shadowfox_botany:lightningRod", localIcon)) {
                this.icon = localIcon
            }
        }
    }

    @SideOnly(Side.CLIENT)
    override fun getIconFromDamage(meta: Int): IIcon {
        return this.icon
    }

    override fun getItemUseAction(par1ItemStack: ItemStack?): EnumAction {
        return EnumAction.bow
    }

    override fun getMaxItemUseDuration(par1ItemStack: ItemStack?): Int {
        return 72000
    }

    override fun onPlayerStoppedUsing(stack: ItemStack?, world: World?, player: EntityPlayer?, count: Int) {
        super.onPlayerStoppedUsing(stack, world, player, count)
        ItemNBTHelper.setInt(stack, "target", -1)
    }

    override fun onUsingTick(stack: ItemStack?, player: EntityPlayer?, count: Int) {
        if (count != this.getMaxItemUseDuration(stack) && !player!!.worldObj.isRemote) {

            var thor = (vazkii.botania.common.item.relic.ItemThorRing.getThorRing(player) != null)
            var priest = (ItemPriestEmblem.getEmblem(0, player) != null)
            var prowess = IManaProficiencyArmor.Helper.hasProficiency(player)

            val color = IPriestColorOverride.getColor(player, 0x0079C4)
            val innerColor = Color(color).brighter().brighter().rgb
            
            if (ManaItemHandler.requestManaExactForTool(stack, player, getCost(thor, prowess, priest), false)) {
                var target = getTarget(player!!.worldObj, player, ItemNBTHelper.getInt(stack, "target", -1))
                if (target != null) {
                    ItemNBTHelper.setInt(stack, "target", target.entityId)

                    val shockspeed = getSpeed(thor, prowess, priest)
                    val damage = getDamage(thor, prowess, priest)

                    val targetCenter = Vector3.fromEntityCenter(target).add(0.0, 0.75, 0.0).add(Vector3(target.lookVec).multiply(-0.25))
                    val targetShift = targetCenter.copy().add(getHeadOrientation(target))

                    val playerCenter = Vector3.fromEntityCenter(player).add(0.0, 0.75, 0.0).add(Vector3(player.lookVec).multiply(-0.25))
                    val playerShift = playerCenter.copy().add(getHeadOrientation(player))
                    if (count % (shockspeed / 10) == 0) {
                        Botania.proxy.lightningFX(player.worldObj, targetCenter, targetShift, 2.0f, color, innerColor)
                        Botania.proxy.lightningFX(player.worldObj, playerCenter, playerShift, 2.0f, color, innerColor)
                    }

                    if (count % shockspeed == 0) {
                        if (ConfigHandler.realLightning && thor) {
                            if (spawnLightning(player.worldObj, target.posX.toDouble(), target.posY.toDouble(), target.posZ.toDouble())) {
                                if (ManaItemHandler.requestManaExactForTool(stack, player, getCost(thor, prowess, priest), true))
                                    target.attackEntityFrom(DamageSource.causePlayerDamage(player), damage)
                            }
                        } else {
                            if (ManaItemHandler.requestManaExactForTool(stack, player, getCost(thor, prowess, priest), true)) {
                                target.attackEntityFrom(DamageSource.causePlayerDamage(player), damage)
                                Botania.proxy.lightningFX(player.worldObj, playerCenter, Vector3.fromEntityCenter(target), 1.0f, color, innerColor)
                                player.worldObj.playSoundEffect(target.posX.toDouble(), target.posY.toDouble(), target.posZ.toDouble(), "ambient.weather.thunder", 100.0f, 0.8f + player.worldObj.rand.nextFloat() * 0.2f)
                            }
                            chainLightning(stack!!, target, player, thor, prowess, priest, color, innerColor)
                        }
                    }

                }
            }
        }
    }

    fun getCost(thor: Boolean, prowess: Boolean, priest: Boolean): Int {
        return COST + (if (thor) THOR_COST else 0) + (if (prowess) PROWESS_COST else 0) + (if (priest) PRIEST_COST else 0)
    }
    fun getSpeed(thor: Boolean, prowess: Boolean, priest: Boolean): Int {
        return SPEED - (if (thor) THOR_SPEEDUP else 0) - (if (prowess) PROWESS_SPEEDUP else 0) - (if (priest) PRIEST_SPEEDUP else 0)
    }
    fun getDamage(thor: Boolean, prowess: Boolean, priest: Boolean): Float {
        return DAMAGE + (if (thor) THOR_POWERUP else 0f) + (if (prowess) PROWESS_POWERUP else 0f) + (if (priest) PRIEST_POWERUP else 0f)
    }
    fun getRange(thor: Boolean, prowess: Boolean, priest: Boolean): Float {
        return CHAINRANGE + (if (thor) THOR_RANGEUP else 0f) + (if (prowess) PROWESS_RANGEUP else 0f) + (if (priest) PRIEST_RANGEUP else 0f)
    }
    fun getTargetCap(thor: Boolean, prowess: Boolean, priest: Boolean): Int {
        return TARGETS + (if (thor) THOR_TARGETS else 0) + (if (prowess) PROWESS_TARGETS else 0) + (if (priest) PRIEST_TARGETS else 0)
    }

    override fun onItemRightClick(par1ItemStack: ItemStack, par2World: World?, par3EntityPlayer: EntityPlayer?): ItemStack {
        par3EntityPlayer!!.setItemInUse(par1ItemStack, this.getMaxItemUseDuration(par1ItemStack))
        return par1ItemStack
    }

    fun getTarget(world: World, player: EntityPlayer, trial_target: Int, range: Float = 12.0f): EntityLivingBase? {
        val selector = object : IEntitySelector {
            override fun isEntityApplicable(e: Entity): Boolean {
                return e is IMob && e !is EntityPlayer && e !is EntityPlayerMP
            }
        }

        val potential = world.selectEntitiesWithinAABB(EntityLivingBase::class.java, AxisAlignedBB.getBoundingBox(player.posX - range, player.posY - range, player.posZ - range, player.posX + range, player.posY + range, player.posZ + range), selector)

        if (trial_target >= 0) {
            var target = world.getEntityByID(trial_target)

            if (target != null && target is EntityCreature)
                if (target.health > 0.0f && !target.isDead && potential.contains(target)) {
                    return target
                }
        }

        if (potential.size > 0)
            while (potential.size > 0) {
                var i = world.rand.nextInt(potential.size)
                if (!(potential[i] as EntityLivingBase).isDead) {
                    return potential[i] as EntityLivingBase
                }

                potential.remove(i)
            }

        return null
    }

    fun chainLightning(stack: ItemStack, entity: EntityLivingBase?, attacker: EntityLivingBase?, thor: Boolean, prowess: Boolean, priest: Boolean, color: Int, innerColor: Int): Boolean {
        if (entity !is EntityPlayer && entity != null) {
            val range = getRange(thor, prowess, priest)
            val targets = getTargetCap(thor, prowess, priest)
            var dmg = getDamage(thor, prowess, priest)

            val alreadyTargetedEntities = ArrayList<Entity>()
            val lightningSeed = ItemNBTHelper.getLong(stack, "lightningSeed", 0L)
            val selector = object : IEntitySelector {
                override fun isEntityApplicable(e: Entity): Boolean {
                    return e is IMob && e !is EntityPlayer && e !is EntityPlayerMP && !alreadyTargetedEntities.contains(e)
                }
            }
            val rand = Random(lightningSeed)
            var lightningSource: EntityLivingBase = entity

            for (i in 0..targets) {
                val entities = entity.worldObj.getEntitiesWithinAABBExcludingEntity(lightningSource, AxisAlignedBB.getBoundingBox(lightningSource.posX - range, lightningSource.posY - range, lightningSource.posZ - range, lightningSource.posX + range, lightningSource.posY + range, lightningSource.posZ + range), selector)
                if (entities.isEmpty()) {
                    break
                }

                val target = entities[rand.nextInt(entities.size)] as EntityLivingBase
                if (attacker != null && attacker is EntityPlayer) {
                    target.attackEntityFrom(DamageSource.causePlayerDamage(attacker as EntityPlayer?), dmg.toFloat())
                } else {
                    target.attackEntityFrom(DamageSource.causeMobDamage(attacker), dmg.toFloat())
                }

                Botania.proxy.lightningFX(entity.worldObj, Vector3.fromEntityCenter(lightningSource), Vector3.fromEntityCenter(target), 1.0f, color, innerColor)
                alreadyTargetedEntities.add(target)
                lightningSource = target
                --dmg
            }

            if (!entity.worldObj.isRemote) {
                ItemNBTHelper.setLong(stack, "lightningSeed", entity.worldObj.rand.nextLong())
            }
        }

        return super.hitEntity(stack, entity, attacker)
    }


    fun spawnLightning(world: World, posX: Double, posY: Double, posZ: Double): Boolean {
        return world.addWeatherEffect(EntityLightningBolt(world, posX, posY, posZ))
    }

    override fun isFull3D(): Boolean {
        return true
    }

    override fun usesMana(stack: ItemStack): Boolean {
        return true
    }

    fun getHeadOrientation(entity: EntityLivingBase): Vector3 {
        val f1 = MathHelper.cos(-entity.rotationYaw * 0.017453292F - Math.PI.toFloat())
        val f2 = MathHelper.sin(-entity.rotationYaw * 0.017453292F - Math.PI.toFloat())
        val f3 = -MathHelper.cos(-(entity.rotationPitch-90) * 0.017453292F)
        val f4 = MathHelper.sin(-(entity.rotationPitch-90) * 0.017453292F)
        return Vector3((f2 * f3).toDouble(), f4.toDouble(), (f1 * f3).toDouble())
    }

    override fun onAvatarUpdate(tile: IAvatarTile, stack: ItemStack) {
        val te = tile as TileEntity
        val world = te.worldObj
        val range = 18

        val color = 0x0079C4
        val innerColor = Color(color).brighter().brighter().rgb

        if (tile.currentMana >= COST_AVATAR && tile.isEnabled && tile.elapsedFunctionalTicks % 10 == 0) {
            val selector = object : IEntitySelector {
                override fun isEntityApplicable(e: Entity): Boolean {
                    return (if (true) e is EntityLivingBase else e is IMob) && e !is EntityPlayer && e !is EntityPlayerMP && e !is EntityDoppleganger
                }
            }

            val entities = world.selectEntitiesWithinAABB(EntityLivingBase::class.java, AxisAlignedBB.getBoundingBox((te.xCoord - range).toDouble(), (te.yCoord - range).toDouble(),
                    (te.zCoord - range).toDouble(), (te.xCoord + range).toDouble(), (te.yCoord + range).toDouble(), (te.zCoord + range).toDouble()), selector)

            if (entities.size == 0) return

            val trial_target = ItemNBTHelper.getInt(stack, "target", -1)
            var target: EntityLivingBase? = null

            if (trial_target >= 0) {
                var ttarget = world.getEntityByID(trial_target)

                if (ttarget != null && ttarget is EntityCreature)
                    if (ttarget.health > 0.0f && !ttarget.isDead && entities.contains(ttarget)) {
                        target = ttarget
                    }
            }

            if (target == null) {
                while (entities.size > 0) {
                    var i = world.rand.nextInt(entities.size)

                    if (entities[i] is EntityLivingBase && entities[i] is IMob && entities[i] !is EntityPlayer && entities[i] !is EntityPlayerMP) {
                        var entity: EntityLivingBase = entities[i] as EntityLivingBase
                        if (entity is EntityLivingBase && !entity.isDead) {
                            target = entity
                            break
                        }
                    }

                    entities.remove(entities[i])
                }
            }

            if (target != null) {
                ItemNBTHelper.setInt(stack, "target", target.entityId)

                val targetCenter = Vector3.fromEntityCenter(target).add(0.0, 0.75, 0.0).add(Vector3(target.lookVec).multiply(-0.25))
                val targetShift = targetCenter.copy().add(getHeadOrientation(target))

                val thisCenter = Vector3.fromTileEntityCenter(te).add(0.0, 0.5, 0.0)
                val thisShift = thisCenter.copy().add(0.0, 1.0, 0.0)

                if (tile.elapsedFunctionalTicks % 10 == 0) {
                    Botania.proxy.lightningFX(world, targetCenter, targetShift, 2.0f, color, innerColor)
                    Botania.proxy.lightningFX(world, thisCenter, thisShift, 2.0f, color, innerColor)
                }

                if (tile.elapsedFunctionalTicks % 100 == 0) {

                    target.attackEntityFrom(DamageSource.causeMobDamage(null), DAMAGE)

                    if (!world.isRemote) tile.recieveMana(-COST_AVATAR)

                    var vect = Vector3.fromTileEntityCenter(te).add(0.0, 0.5, 0.0)

                    Botania.proxy.lightningFX(world, vect, Vector3.fromEntityCenter(target), 1.0f, color, innerColor)

                    world.playSoundEffect(target.posX.toDouble(), target.posY.toDouble(), target.posZ.toDouble(), "ambient.weather.thunder", 100.0f, 0.8f + world.rand.nextFloat() * 0.2f)

                    chainLightning(stack, target, null, false, false, false, color, innerColor)
                }
            }

        }
    }

    override fun getOverlayResource(tile: IAvatarTile, stack: ItemStack): ResourceLocation {
        return avatarOverlay
    }
}
