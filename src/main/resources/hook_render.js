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
                var MethodInsnNode = Java.type("org.objectweb.asm.tree.MethodInsnNode");
                var asmapi = Java.type('net.minecraftforge.coremod.api.ASMAPI');
                var opcodes = Java.type('org.objectweb.asm.Opcodes');

                asmapi.log("INFO", "[Deck Native Controls] GameRenderer.render transformer");

                for (var i = 0; i < methodNode.instructions.size(); i++) {
                    var opc = methodNode.instructions.get(i);
                    if (opc.getOpcode() === opcodes.RETURN) {
                        asmapi.log("DEBUG", "[Deck Native Controls] found ret");

                        var new_opc = new MethodInsnNode(
                            opcodes.INVOKESTATIC,
                            "nekomods/DeckControls",
                            "renderOverlay",
                            "()V"
                        );
                        methodNode.instructions.insertBefore(opc, new_opc);

                        break;
                    }
                }

                return methodNode;
            }
        }
    }
}
