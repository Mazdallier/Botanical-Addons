package ninja.shadowfox.shadowfox_botany.common.entity

import net.minecraft.entity.Entity
import net.minecraft.entity.effect.EntityLightningBolt
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.AxisAlignedBB
import net.minecraft.world.World
import ninja.shadowfox.shadowfox_botany.common.blocks.BlockLightningRod
import ninja.shadowfox.shadowfox_botany.common.utils.centerVector
import vazkii.botania.common.Botania
import java.util.*


class EntityLightningRod() : TileEntity() {

    override fun updateEntity() {
        if (worldObj != null) {
            if (worldObj.getBlock(xCoord, yCoord + 1, zCoord) !is BlockLightningRod) {
                for (e in getLightningBoltsWithinAABB(worldObj, AxisAlignedBB.getBoundingBox((xCoord - 30).toDouble(), (yCoord - 30).toDouble(), (zCoord - 30).toDouble(),
                        (xCoord + 30).toDouble(), (yCoord + 30).toDouble(), (zCoord + 30).toDouble()))) {
                    worldObj.removeEntity(e)

                    val wispLoc = this.centerVector()

                    Botania.proxy.wispFX(worldObj, wispLoc.x, wispLoc.y - 4, wispLoc.z, 1f, 1f, 1f, 5f)
                    worldObj.addWeatherEffect(FakeLightning(worldObj, xCoord.toDouble(), (yCoord + 1).toDouble(), zCoord.toDouble()))
                }
            }
        }
    }

    fun getLightningBoltsWithinAABB(world: World, box: AxisAlignedBB): ArrayList<EntityLightningBolt> {
        var bolts = ArrayList<EntityLightningBolt>()

        for (effect in world.weatherEffects) {
            if (effect is EntityLightningBolt && effect !is FakeLightning) {
                bolts.add(effect)
            }
        }

        return bolts
    }

    /**
     * Like real lightning but less fire
     */
    class FakeLightning(world: World, x: Double, y: Double, z: Double) : EntityLightningBolt(world, x , y, z) {
        private var lightningState: Int = 0
        private var boltLivingTime: Int = 0

        init {
            this.setLocationAndAngles(x, y, z, 0.0f, 0.0f)
            this.lightningState = 2
            this.boltVertex = this.rand.nextLong()
            this.boltLivingTime = this.rand.nextInt(3) + 1
        }

        override fun onUpdate() {
            super.onUpdate()

            if (this.lightningState == 2) {
                this.worldObj.playSoundEffect(this.posX, this.posY, this.posZ, "ambient.weather.thunder", 10000.0f, 0.8f + this.rand.nextFloat() * 0.2f)
                this.worldObj.playSoundEffect(this.posX, this.posY, this.posZ, "random.explode", 2.0f, 0.5f + this.rand.nextFloat() * 0.2f)
            }

            --this.lightningState

            if (this.lightningState < 0) {
                if (this.boltLivingTime == 0) {
                    this.setDead()
                } else if (this.lightningState < -this.rand.nextInt(10)) {
                    --this.boltLivingTime
                    this.lightningState = 1
                    this.boltVertex = this.rand.nextLong()
                }
            }

            if (this.lightningState >= 0) {
                if (this.worldObj.isRemote) {
                    this.worldObj.lastLightningBolt = 2
                } else {
                    val d0 = 3.0
                    val list = this.worldObj.getEntitiesWithinAABBExcludingEntity(this, AxisAlignedBB.getBoundingBox(this.posX - d0, this.posY - d0, this.posZ - d0, this.posX + d0, this.posY + 6.0 + d0, this.posZ + d0))

                    for (l in list.indices) {
                        val entity = list[l] as Entity
                        if (!net.minecraftforge.event.ForgeEventFactory.onEntityStruckByLightning(entity, this))
                            entity.onStruckByLightning(this)
                    }
                }
            }
        }

        override fun entityInit() {}
        override fun readEntityFromNBT(p_70037_1_: NBTTagCompound) {}
        override fun writeEntityToNBT(p_70014_1_: NBTTagCompound) {}
    }
}
