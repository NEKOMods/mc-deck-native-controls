function initializeCoreMod() {
    return {
        'hook_isKeyDown': {
            'target': {
                'type': 'METHOD',
                'class': 'com.mojang.blaze3d.platform.InputConstants',
                'methodName': 'm_84830_',
                'methodDesc': '(JI)Z'
            },
            'transformer': function(methodNode) {
                var InsnList = Java.type("org.objectweb.asm.tree.InsnList");
                var MethodInsnNode = Java.type("org.objectweb.asm.tree.MethodInsnNode");
                var VarInsnNode = Java.type("org.objectweb.asm.tree.VarInsnNode");
                var asmapi = Java.type('net.minecraftforge.coremod.api.ASMAPI');
                var opcodes = Java.type('org.objectweb.asm.Opcodes');

                asmapi.log("INFO", "[Deck Native Controls] InputConstants.isKeyDown transformer");

                for (var i = 0; i < methodNode.instructions.size(); i++) {
                    var opc = methodNode.instructions.get(i);
                    if (opc.getOpcode() === opcodes.IRETURN) {
                        asmapi.log("DEBUG", "[Deck Native Controls] found ret");

                        var il = new InsnList();
                        il.add(new VarInsnNode(opcodes.ILOAD, 2));
                        il.add(new MethodInsnNode(
                            opcodes.INVOKESTATIC,
                            "nekomods/deckcontrols/InputHooks",
                            "hookKeyDown",
                            "(ZI)Z"
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
