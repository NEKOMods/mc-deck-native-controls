function initializeCoreMod() {
    return {
        'hook_runTick': {
            'target': {
                'type': 'METHOD',
                'class': 'net.minecraft.client.Minecraft',
                'methodName': 'm_91383_',
                'methodDesc': '(Z)V'
            },
            'transformer': function(methodNode) {
                var MethodInsnNode = Java.type("org.objectweb.asm.tree.MethodInsnNode");
                var asmapi = Java.type('net.minecraftforge.coremod.api.ASMAPI');
                var opcodes = Java.type('org.objectweb.asm.Opcodes');

                asmapi.log("INFO", "[Deck Native Controls] Minecraft.runTick transformer");

                var turnplayer = asmapi.findFirstMethodCall(
                    methodNode,
                    asmapi.MethodType.VIRTUAL,
                    "net/minecraft/client/MouseHandler",
                    asmapi.mapMethod("m_91523_"),
                    "()V"
                );
                if (turnplayer === null) {
                    asmapi.log("ERROR", "[Deck Native Controls] couldn't find turnPlayer");
                    return methodNode;
                }

                asmapi.log("DEBUG", "[Deck Native Controls] found turnPlayer");
                if (turnplayer.getPrevious().getOpcode() !== opcodes.GETFIELD) {
                    asmapi.log("ERROR", "[Deck Native Controls] expected GETFIELD");
                    return methodNode;
                }
                if (turnplayer.getPrevious().getPrevious().getOpcode() !== opcodes.ALOAD) {
                    asmapi.log("ERROR", "[Deck Native Controls] expected ALOAD");
                    return methodNode;
                }

                var new_opc = new MethodInsnNode(
                    opcodes.INVOKESTATIC,
                    "nekomods/deckcontrols/InputHooks",
                    "runTickHook",
                    "()V"
                );
                methodNode.instructions.insertBefore(turnplayer.getPrevious().getPrevious(), new_opc);

                return methodNode;
            }
        }
    }
}
