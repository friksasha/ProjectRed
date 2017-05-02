/*
 * Copyright (c) 2015.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.projectred.expansion

import codechicken.lib.data.{MCDataInput, MCDataOutput}
import codechicken.lib.render.uv.{MultiIconTransformation, UVTransformation}

import codechicken.lib.lighting.LightModel
import codechicken.lib.render.{CCModel, CCRenderState, TextureUtils}
import codechicken.lib.vec._
import codechicken.microblock.FaceMicroClass
import codechicken.multipart.{MultiPartRegistry, TItemMultiPart, TMultiPart}

import cpw.mods.fml.relauncher.{Side, SideOnly}
import mrtjp.core.color.Colors
import mrtjp.core.inventory.TInventory
import mrtjp.core.render.TCubeMapRender
import mrtjp.core.vec.Point
import mrtjp.core.world.WorldLib
import mrtjp.projectred.core.PartDefs
import mrtjp.projectred.core.libmc.PRResources
import mrtjp.projectred.{ProjectRedCore, ProjectRedExpansion}
import net.minecraft.client.renderer.texture.IIconRegister
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.ICrafting
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.IIcon
import net.minecraft.world.IBlockAccess
import net.minecraft.world.World

class TileSolarPanel extends TPoweredMachine
{
    var isBurning = false
    var isCharged = false
    var burnTimeRemaining = 0
    var storage = 0

    override def save(tag:NBTTagCompound)
    {
        super.save(tag)
        tag.setInteger("storage", storage)
        tag.setShort("btime", burnTimeRemaining.toShort)
    }

    override def load(tag:NBTTagCompound)
    {
        super.load(tag)
        storage = tag.getInteger("storage")
        burnTimeRemaining = tag.getShort("btime")
        isBurning = burnTimeRemaining > 0
        isCharged = cond.canWork
        ib = isBurning
        ic = isCharged
    }

    override def writeDesc(out:MCDataOutput)
    {
        super.writeDesc(out)
        out.writeBoolean(isCharged)
        out.writeBoolean(isBurning)
    }

    override def readDesc(in:MCDataInput)
    {
        super.readDesc(in)
        isCharged = in.readBoolean()
        isBurning = in.readBoolean()
    }

    override def read(in:MCDataInput, key:Int) = key match
    {
        case 5 =>
            isCharged = in.readBoolean()
            isBurning = in.readBoolean()
            markRender()
            markLight()
        case _ => super.read(in, key)
    }

    def sendRenderUpdate()
    {
        writeStream(5).writeBoolean(isCharged).writeBoolean(isBurning).sendToChunk()
    }

    override def getBlock = ProjectRedExpansion.machine1

//    override def size = 1
//    override def name = "Solar_panel"

    def getStorageScaled(i:Int) = math.min(i, i*storage/getMaxStorage)

    def getMaxStorage = 10
    def getDrawSpeed = 100
    def getDrawFloor = 1000

    override def update()
    {
        super.update()
		tryChargeStorage()
        tryChargeConductor()
    }

	def visibilityMultiplier =
	if (world.canBlockSeeTheSky(x, y, z)) 1.0
    else if (world.canBlockSeeTheSky(x, y+1, z) && !world.getBlock(x, y+1, z).getMaterial.isOpaque) 0.7
    else 0.0


    def tryChargeStorage()
    {
        if (storage < getMaxStorage)
        {
			val t = world.getWorldTime%24000
			if (t < 12000)
			{
				val rainMultiplier = 1.0-world.rainingStrength
				if (rainMultiplier == 1)
				{
					visibilityMultiplier
					if (visibilityMultiplier != 0)
					{
						storage += 1
					}
				}
			}
        }
    }
	

    def tryChargeConductor()
    {
        if (cond.charge < getDrawFloor && storage > 0)
        {
            var n = math.min(getDrawFloor-cond.charge, getDrawSpeed)/10
            n = math.min(n, storage)
            cond.applyPower(n*1000)
            storage -= n
        }
    }

    private var ib = false
    private var ic = false

    override def onBlockRemoval()
    {
        super.onBlockRemoval()
    }
}

object RenderSolarPanel extends TCubeMapRender
{
    var side1:IIcon = _
    var top:IIcon = _
    var bottom:IIcon = _

    var iconT:UVTransformation = _

    override def getData(w:IBlockAccess, x:Int, y:Int, z:Int) =
    {
		(0, 0, iconT)
    }

    override def getInvData = (0, 0, iconT)

    override def getIcon(side:Int, meta:Int) = side match
    {
        case 0 => bottom
        case 1 => top
        case _ => side1
    }

    override def registerIcons(reg:IIconRegister)
    {
        side1 = reg.registerIcon("projectred:mechanical/solar/side")
        top = reg.registerIcon("projectred:mechanical/solar/top")
        bottom = reg.registerIcon("projectred:mechanical/solar/bottom")

        iconT = new MultiIconTransformation(bottom, top, side1, side1, side1, side1)
    }
}