function initializeCoreMod() {
    return {
        'hook_render': {
            'target': {
                'type': 'METHOD',
                'class': 'net.minecraft.client.renderer.GameRenderer',
                'methodName': 'm_109093_',
                'methodDesc': '(FJZ)V'
            },
            'transformer': function(methodNode) {
                var InsnList = Java.type("org.objectweb.asm.tree.InsnList");
                var MethodInsnNode = Java.type("org.objectweb.asm.tree.MethodInsnNode");
                var VarInsnNode = Java.type("org.objectweb.asm.tree.VarInsnNode");
                var asmapi = Java.type('net.minecraftforge.coremod.api.ASMAPI');
                var opcodes = Java.type('org.objectweb.asm.Opcodes');

                asmapi.log("INFO", "[Deck Native Controls] GameRenderer.render transformer");

                for (var i = 0; i < methodNode.instructions.size(); i++) {
                    var opc = methodNode.instructions.get(i);
                    if (opc.getOpcode() === opcodes.RETURN) {
                        asmapi.log("DEBUG", "[Deck Native Controls] found ret");

                        var il = new InsnList();
                        il.add(new VarInsnNode(opcodes.FLOAD, 1));
                        il.add(new MethodInsnNode(
                            opcodes.INVOKESTATIC,
                            "nekomods/deckcontrols/OverlayRenderer",
                            "renderOverlay",
                            "(F)V"
                        ));
                        methodNode.instructions.insertBefore(opc, il);

                        break;
                    }
                }

                return methodNode;
            }
        }
    }
}
