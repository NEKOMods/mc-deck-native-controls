function initializeCoreMod() {
    return {
        'hook_player_impulse': {
            'target': {
                'type': 'METHOD',
                'class': 'net.minecraft.client.player.KeyboardInput',
                'methodName': 'm_214106_',
                'methodDesc': '(ZF)V'
            },
            'transformer': function(methodNode) {
                var MethodInsnNode = Java.type("org.objectweb.asm.tree.MethodInsnNode");
                var asmapi = Java.type('net.minecraftforge.coremod.api.ASMAPI');
                var opcodes = Java.type('org.objectweb.asm.Opcodes');

                asmapi.log("INFO", "[Deck Native Controls] KeyboardInput.tick transformer");

                for (var i = 0; i < methodNode.instructions.size(); i++) {
                    var opc = methodNode.instructions.get(i);
                    if (opc.getOpcode() === opcodes.PUTFIELD) {
                        if (opc.owner === "net/minecraft/client/player/KeyboardInput" && opc.name === asmapi.mapField("f_108567_")) {
                            asmapi.log("DEBUG", "[Deck Native Controls] found forwardImpulse at " + i);

                            var new_opc = new MethodInsnNode(
                                opcodes.INVOKESTATIC,
                                "nekomods/deckcontrols/InputHooks",
                                "playerFBImpulse",
                                "(F)F"
                            );
                            methodNode.instructions.insertBefore(opc, new_opc);

                            break;
                        }
                    }
                }

                for (var i = 0; i < methodNode.instructions.size(); i++) {
                    var opc = methodNode.instructions.get(i);
                    if (opc.getOpcode() === opcodes.PUTFIELD) {
                        if (opc.owner === "net/minecraft/client/player/KeyboardInput" && opc.name === asmapi.mapField("f_108566_")) {
                            asmapi.log("DEBUG", "[Deck Native Controls] found leftImpulse at " + i);

                            var new_opc = new MethodInsnNode(
                                opcodes.INVOKESTATIC,
                                "nekomods/deckcontrols/InputHooks",
                                "playerLRImpulse",
                                "(F)F"
                            );
                            methodNode.instructions.insertBefore(opc, new_opc);

                            break;
                        }
                    }
                }

                return methodNode;
            }
        }
    }
}
