package com.hbm.inventory.gui;

import org.lwjgl.opengl.GL11;

import com.hbm.handler.RocketStruct;
import com.hbm.inventory.container.ContainerMachineRocketAssembly;
import com.hbm.lib.RefStrings;
import com.hbm.packet.NBTControlPacket;
import com.hbm.packet.PacketDispatcher;
import com.hbm.render.util.MissilePronter;
import com.hbm.tileentity.machine.TileEntityMachineRocketAssembly;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;

public class GUIMachineRocketAssembly extends GuiInfoContainerLayered {

	private static ResourceLocation texture = new ResourceLocation(RefStrings.MODID + ":textures/gui/machine/gui_rocket_assembly.png");

	private TileEntityMachineRocketAssembly machine;

	private double currentOffset = 0;
	private double currentScale = 1;
	private long lastTime = 0;

	public GUIMachineRocketAssembly(InventoryPlayer invPlayer, TileEntityMachineRocketAssembly machine) {
		super(new ContainerMachineRocketAssembly(invPlayer, machine));
		this.machine = machine;
		
		this.xSize = 176;
		this.ySize = 224;
	}

	@Override
	public void initGui() {
        super.initGui();
		lastTime = System.nanoTime();
    }

	@Override
	protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
		drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);

		int stage = Math.max(machine.rocket.stages.size() - 1 - getLayer(), -1);

		drawTexturedModalRect(guiLeft + 47, guiTop + 39, 194 + (stage + 1) * 6, 0, 6, 8);

		stage = Math.max(stage, 0);

		double dt = (double)(System.nanoTime() - lastTime) / 1000000000;
		lastTime = System.nanoTime();
		
		GL11.glPushMatrix();
		{

			pushScissor(65, 5, 106, 106);

			GL11.glTranslatef(guiLeft + 116, guiTop + 103, 100);
			GL11.glRotatef(System.currentTimeMillis() / 10 % 360, 0, -1, 0);
			
			double size = 86;
			double height = machine.rocket.getHeight(stage);
			double targetScale = size / Math.max(height, 6);
			currentScale = currentScale + (targetScale - currentScale) * dt * 4;

			double targetOffset = machine.rocket.getOffset(stage);
			currentOffset = currentOffset + (targetOffset - currentOffset) * dt * 4;
			
			GL11.glScaled(-currentScale, -currentScale, -currentScale);
			GL11.glTranslated(0, -currentOffset, 0);

			MissilePronter.prontRocket(machine.rocket, Minecraft.getMinecraft().getTextureManager());

			popScissor();

		}
		GL11.glPopMatrix();
	}

	@Override
	protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		Minecraft.getMinecraft().getTextureManager().bindTexture(texture);

		if(checkClick(mouseX, mouseY, 17, 34, 18, 8)) {
			drawTexturedModalRect(17, 34, 176, 36, 18, 8);
		}
		
		if(checkClick(mouseX, mouseY, 17, 98, 18, 8)) {
			drawTexturedModalRect(17, 98, 176, 44, 18, 8);
		}

		if(machine.rocket.validate()) {
			drawTexturedModalRect(41, 62, 194, 8, 18, 18);
		}
	}

	@Override
	protected void mouseClicked(int x, int y, int i) {
    	super.mouseClicked(x, y, i);
		
		// Stage up
    	if(checkClick(x, y, 17, 34, 18, 8)) {
			mc.getSoundHandler().playSound(PositionedSoundRecord.func_147674_a(new ResourceLocation("gui.button.press"), 1.0F));

			if(getLayer() > 0) {
				setLayer(getLayer() - 1);
			}
    	}

		// Stage down
		if(checkClick(x, y, 17, 98, 18, 8)) {
			mc.getSoundHandler().playSound(PositionedSoundRecord.func_147674_a(new ResourceLocation("gui.button.press"), 1.0F));

			if(getLayer() < Math.min(machine.rocket.stages.size(), RocketStruct.MAX_STAGES - 1)) {
				setLayer(getLayer() + 1);
			}
    	}

		// Construct rocket
		if(machine.rocket.validate() && checkClick(x, y, 41, 62, 18, 18)) {
			mc.getSoundHandler().playSound(PositionedSoundRecord.func_147674_a(new ResourceLocation("gui.button.press"), 1.0F));
			NBTTagCompound data = new NBTTagCompound();
			data.setBoolean("construct", true);
			PacketDispatcher.wrapper.sendToServer(new NBTControlPacket(data, machine.xCoord, machine.yCoord, machine.zCoord));
		}
    }
		
}