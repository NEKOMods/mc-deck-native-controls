package nekomods;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;

public class OverlayRenderer {
    public static void renderOverlay() {
        PoseStack ps = new PoseStack();
        HidInput.HidState hidState = HidInput.latestInput;
        String dbgText =
                ((hidState.buttons & HidInput.HidState.BTN_D_UP) != 0 ? "U" : " ") +
                ((hidState.buttons & HidInput.HidState.BTN_D_DOWN) != 0 ? "D" : " ") +
                ((hidState.buttons & HidInput.HidState.BTN_D_LEFT) != 0 ? "L" : " ") +
                ((hidState.buttons & HidInput.HidState.BTN_D_RIGHT) != 0 ? "R" : " ") +
                ((hidState.buttons & HidInput.HidState.BTN_A) != 0 ? "A" : " ") +
                ((hidState.buttons & HidInput.HidState.BTN_B) != 0 ? "B" : " ") +
                ((hidState.buttons & HidInput.HidState.BTN_X) != 0 ? "X" : " ") +
                ((hidState.buttons & HidInput.HidState.BTN_Y) != 0 ? "Y" : " ") +
                ((hidState.buttons & HidInput.HidState.BTN_L4) != 0 ? "4" : " ") +
                ((hidState.buttons & HidInput.HidState.BTN_L5) != 0 ? "5" : " ") +
                ((hidState.buttons & HidInput.HidState.BTN_R4) != 0 ? "$" : " ") +
                ((hidState.buttons & HidInput.HidState.BTN_R5) != 0 ? "%" : " ") +
                ((hidState.buttons & HidInput.HidState.BTN_VIEW) != 0 ? "V" : " ") +
                ((hidState.buttons & HidInput.HidState.BTN_OPTIONS) != 0 ? "O" : " ") +
                ((hidState.buttons & HidInput.HidState.BTN_STEAM) != 0 ? "S" : " ") +
                ((hidState.buttons & HidInput.HidState.BTN_DOTS) != 0 ? ":" : " ") +
                ((hidState.buttons & HidInput.HidState.BTN_LTHUMB_TOUCH) != 0 ? "q" : " ") +
                ((hidState.buttons & HidInput.HidState.BTN_RTHUMB_TOUCH) != 0 ? "p" : " ") +
                ((hidState.buttons & HidInput.HidState.BTN_LPAD_TOUCH) != 0 ? "w" : " ") +
                ((hidState.buttons & HidInput.HidState.BTN_RPAD_TOUCH) != 0 ? "o" : " ") +
                ((hidState.buttons & HidInput.HidState.BTN_LTHUMB_CLICK) != 0 ? "Q" : " ") +
                ((hidState.buttons & HidInput.HidState.BTN_RTHUMB_CLICK) != 0 ? "P" : " ") +
                ((hidState.buttons & HidInput.HidState.BTN_LPAD_CLICK) != 0 ? "W" : " ") +
                ((hidState.buttons & HidInput.HidState.BTN_RPAD_CLICK) != 0 ? "O" : " ") +
                ((hidState.buttons & HidInput.HidState.BTN_LT_DIGITAL) != 0 ? "1" : " ") +
                ((hidState.buttons & HidInput.HidState.BTN_RT_DIGITAL) != 0 ? "2" : " ") +
                ((hidState.buttons & HidInput.HidState.BTN_LT_ANALOG_FULL) != 0 ? "!" : " ") +
                ((hidState.buttons & HidInput.HidState.BTN_RT_ANALOG_FULL) != 0 ? "@" : " ");
        Minecraft.getInstance().font.draw(ps, dbgText, 0, 0, 0x00ff00);
        dbgText =
                hidState.frame + " " +
                hidState.ltrig + " " + hidState.rtrig + " " +
                "(" + hidState.lthumb_x + ", " + hidState.lthumb_y + ") " +
                "(" + hidState.rthumb_x + ", " + hidState.rthumb_y + ") " +
                "(" + hidState.lpad_x + ", " + hidState.lpad_y + ") " +
                "(" + hidState.rpad_x + ", " + hidState.rpad_y + ") " +
                "(" + hidState.accel_x + ", " + hidState.accel_y + ", " + hidState.accel_z + ") " +
                "(" + hidState.gyro_yaw + ", " + hidState.gyro_pitch + ", " + hidState.gyro_roll + ") " +
                "(" + hidState.pose_quat_w + ", " + hidState.pose_quat_x + ", " + hidState.pose_quat_y + ", " + hidState.pose_quat_z + ") ";
        Minecraft.getInstance().font.draw(ps, dbgText, 0, 16, 0x00ff00);
        dbgText =
                hidState.lpad_force + " " +
                hidState.rpad_force + " " +
                hidState.lthumb_capa + " " +
                hidState.rthumb_capa;
        Minecraft.getInstance().font.draw(ps, dbgText, 0, 32, 0x00ff00);

//        RenderSystem.setShader(GameRenderer::getPositionTexShader);
//        RenderSystem.disableDepthTest();
//        RenderSystem.enableBlend();
//        RenderSystem.defaultBlendFunc();
//        RenderSystem.setShaderTexture(0, new ResourceLocation("textures/gui/book.png"));

//        BufferBuilder b = Tesselator.getInstance().getBuilder();
//        b.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
//        b.vertex(ps.last().pose(), 10, 10, 0).uv(0, 0).endVertex();
//        b.vertex(ps.last().pose(), 10, 60, 0).uv(0, 1).endVertex();
//        b.vertex(ps.last().pose(), 60, 60, 0).uv(1, 1).endVertex();
//        b.vertex(ps.last().pose(), 60, 10, 0).uv(1, 0).endVertex();
//        BufferUploader.drawWithShader(b.end());
    }
}
