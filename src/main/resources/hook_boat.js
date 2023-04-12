function initializeCoreMod() {
    return {
        'hook_boat': {
            'target': {
                'type': 'CLASS',
                'name': 'net.minecraft.world.entity.vehicle.Boat'
            },
            'transformer': function(classNode) {
                var InsnList = Java.type("org.objectweb.asm.tree.InsnList");
                var FieldNode = Java.type("org.objectweb.asm.tree.FieldNode");
                var MethodNode = Java.type("org.objectweb.asm.tree.MethodNode");
                var InsnNode = Java.type("org.objectweb.asm.tree.InsnNode");
                var FieldInsnNode = Java.type("org.objectweb.asm.tree.FieldInsnNode");
                var JumpInsnNode = Java.type("org.objectweb.asm.tree.JumpInsnNode");
                var LabelNode = Java.type("org.objectweb.asm.tree.LabelNode");
                var MethodInsnNode = Java.type("org.objectweb.asm.tree.MethodInsnNode");
                var VarInsnNode = Java.type("org.objectweb.asm.tree.VarInsnNode");
                var asmapi = Java.type('net.minecraftforge.coremod.api.ASMAPI');
                var opcodes = Java.type('org.objectweb.asm.Opcodes');

                asmapi.log("INFO", "[Deck Native Controls] Boat transformer");

                var analogInputLeftRight = new FieldNode(
                    opcodes.ACC_PRIVATE,
                    "analogInputLeftRight",
                    "F", "F", 0);
                var analogInputUpDown = new FieldNode(
                    opcodes.ACC_PRIVATE,
                    "analogInputUpDown",
                    "F", "F", 0);
                var analogInputValid = new FieldNode(
                    opcodes.ACC_PRIVATE,
                    "analogInputValid",
                    "Z", "Z", 0);
                classNode.fields.add(analogInputLeftRight);
                classNode.fields.add(analogInputUpDown);
                classNode.fields.add(analogInputValid);
                asmapi.log("DEBUG", "[Deck Native Controls] added new fields");

                for (var i = 0; i < classNode.methods.size(); i++) {
                    var method = classNode.methods[i];

                    if (method.name == asmapi.mapMethod("m_38342_")) {
                        asmapi.log("DEBUG", "[Deck Native Controls] found Boat.setInput");

                        for (var j = 0; j < method.instructions.size(); j++) {
                            var opc = method.instructions.get(j);
                            if (opc.getOpcode() === opcodes.RETURN) {
                                asmapi.log("DEBUG", "[Deck Native Controls] found ret");

                                var il = new InsnList();
                                il.add(new VarInsnNode(opcodes.ALOAD, 0));
                                il.add(new InsnNode(opcodes.ICONST_0));
                                il.add(new FieldInsnNode(
                                    opcodes.PUTFIELD,
                                    "net/minecraft/world/entity/vehicle/Boat",
                                    "analogInputValid",
                                    "Z"
                                ));
                                method.instructions.insertBefore(opc, il);

                                break;
                            }
                        }
                    }

                    if (method.name == asmapi.mapMethod("m_38396_")) {
                        asmapi.log("DEBUG", "[Deck Native Controls] found Boat.controlBoat");

                        var L_no_analog_controls = new LabelNode();
                        var il = new InsnList();
                        // if statement
                        il.add(new VarInsnNode(opcodes.ALOAD, 0));
                        il.add(new FieldInsnNode(
                            opcodes.GETFIELD,
                            "net/minecraft/world/entity/vehicle/Boat",
                            "analogInputValid",
                            "Z"
                        ));
                        il.add(new JumpInsnNode(
                            opcodes.IFEQ,
                            L_no_analog_controls
                        ));
                        // method call
                        il.add(new VarInsnNode(opcodes.ALOAD, 0));
                        il.add(new VarInsnNode(opcodes.ALOAD, 0));
                        il.add(new FieldInsnNode(
                            opcodes.GETFIELD,
                            "net/minecraft/world/entity/vehicle/Boat",
                            "analogInputLeftRight",
                            "F"
                        ));
                        il.add(new VarInsnNode(opcodes.ALOAD, 0));
                        il.add(new FieldInsnNode(
                            opcodes.GETFIELD,
                            "net/minecraft/world/entity/vehicle/Boat",
                            "analogInputUpDown",
                            "F"
                        ));
                        il.add(new MethodInsnNode(
                            opcodes.INVOKESTATIC,
                            "nekomods/deckcontrols/InputHooks",
                            "hookControlBoat",
                            "(Lnet/minecraft/world/entity/vehicle/Boat;FF)V"
                        ));
                        il.add(new InsnNode(opcodes.RETURN));
                        // end of if
                        il.add(L_no_analog_controls);
                        method.instructions.insert(il);
                    }
                }

                var setInputAnalog = new MethodNode(
                    opcodes.ACC_PUBLIC,
                    "setInputAnalog",
                    "(FF)V",
                    null, null);
                var il = new InsnList();
                il.add(new VarInsnNode(opcodes.ALOAD, 0));
                il.add(new VarInsnNode(opcodes.FLOAD, 1));
                il.add(new FieldInsnNode(
                    opcodes.PUTFIELD,
                    "net/minecraft/world/entity/vehicle/Boat",
                    "analogInputLeftRight",
                    "F"
                ));
                il.add(new VarInsnNode(opcodes.ALOAD, 0));
                il.add(new VarInsnNode(opcodes.FLOAD, 2));
                il.add(new FieldInsnNode(
                    opcodes.PUTFIELD,
                    "net/minecraft/world/entity/vehicle/Boat",
                    "analogInputUpDown",
                    "F"
                ));
                il.add(new VarInsnNode(opcodes.ALOAD, 0));
                il.add(new InsnNode(opcodes.ICONST_1));
                il.add(new FieldInsnNode(
                    opcodes.PUTFIELD,
                    "net/minecraft/world/entity/vehicle/Boat",
                    "analogInputValid",
                    "Z"
                ));
                il.add(new InsnNode(opcodes.RETURN));
                setInputAnalog.instructions.insert(il);
                classNode.methods.add(setInputAnalog);
                asmapi.log("DEBUG", "[Deck Native Controls] added setInputAnalog method");

                return classNode;
            }
        },
        'hook_localplayer_for_boat': {
            'target': {
                'type': 'METHOD',
                'class': 'net.minecraft.client.player.LocalPlayer',
                'methodName': 'm_6083_',
                'methodDesc': '()V'
            },
            'transformer': function(methodNode) {
                var InsnList = Java.type("org.objectweb.asm.tree.InsnList");
                var InsnNode = Java.type("org.objectweb.asm.tree.InsnNode");
                var FieldInsnNode = Java.type("org.objectweb.asm.tree.FieldInsnNode");
                var JumpInsnNode = Java.type("org.objectweb.asm.tree.JumpInsnNode");
                var LabelNode = Java.type("org.objectweb.asm.tree.LabelNode");
                var MethodInsnNode = Java.type("org.objectweb.asm.tree.MethodInsnNode");
                var TypeInsnNode = Java.type("org.objectweb.asm.tree.TypeInsnNode");
                var VarInsnNode = Java.type("org.objectweb.asm.tree.VarInsnNode");
                var asmapi = Java.type('net.minecraftforge.coremod.api.ASMAPI');
                var opcodes = Java.type('org.objectweb.asm.Opcodes');

                asmapi.log("INFO", "[Deck Native Controls] LocalPlayer.rideTick transformer for boats");

                for (var i = 0; i < methodNode.instructions.size(); i++) {
                    var opc = methodNode.instructions.get(i);
                    if (opc.getOpcode() === opcodes.INSTANCEOF && opc.desc === "net/minecraft/world/entity/vehicle/Boat") {
                        asmapi.log("DEBUG", "[Deck Native Controls] found instanceof Boat");

                        if (i + 1 >= methodNode.instructions.size()) {
                            asmapi.log("ERROR", "[Deck Native Controls] Next opcode was not as expected!");
                            break;
                        }
                        var next_opc = methodNode.instructions.get(i + 1);
                        if (next_opc.getOpcode() !== opcodes.IFEQ) {
                            asmapi.log("ERROR", "[Deck Native Controls] Next opcode was not as expected!");
                            break;
                        }
                        var L_not_a_boat = next_opc.label;
                        asmapi.log("DEBUG", "not a boat? " + L_not_a_boat);

                        var L_boring_boats = new LabelNode();
                        var il = new InsnList();
                        // if statement
                        il.add(new MethodInsnNode(
                            opcodes.INVOKESTATIC,
                            "nekomods/deckcontrols/InputHooks",
                            "hookRideTickBoatActive",
                            "()Z"
                        ));
                        il.add(new JumpInsnNode(
                            opcodes.IFEQ,
                            L_boring_boats
                        ));
                        // do our fancy boats
                        // handsBusy = true;
                        il.add(new VarInsnNode(opcodes.ALOAD, 0));
                        il.add(new InsnNode(opcodes.ICONST_1));
                        il.add(new FieldInsnNode(
                            opcodes.PUTFIELD,
                            "net/minecraft/client/player/LocalPlayer",
                            asmapi.mapField("f_108611_"),
                            "Z"
                        ));
                        // call setInputAnalog
                        il.add(new VarInsnNode(opcodes.ALOAD, 0));
                        il.add(new MethodInsnNode(
                            opcodes.INVOKEVIRTUAL,
                            "net/minecraft/client/player/LocalPlayer",
                            asmapi.mapMethod("m_20202_"),   // getVehicle
                            "()Lnet/minecraft/world/entity/Entity;"
                        ));
                        il.add(new TypeInsnNode(
                            opcodes.CHECKCAST,
                            "net/minecraft/world/entity/vehicle/Boat"
                        ));
                        il.add(new MethodInsnNode(
                            opcodes.INVOKESTATIC,
                            "nekomods/deckcontrols/InputHooks",
                            "hookRideTickBoatLeftRight",
                            "()F"
                        ));
                        il.add(new MethodInsnNode(
                            opcodes.INVOKESTATIC,
                            "nekomods/deckcontrols/InputHooks",
                            "hookRideTickBoatUpDown",
                            "()F"
                        ));
                        il.add(new MethodInsnNode(
                            opcodes.INVOKEVIRTUAL,
                            "net/minecraft/world/entity/vehicle/Boat",
                            "setInputAnalog",
                            "(FF)V"
                        ));
                        il.add(new JumpInsnNode(
                            opcodes.GOTO,
                            L_not_a_boat
                        ));
                        il.add(L_boring_boats);
                        methodNode.instructions.insert(next_opc, il);

                        break;
                    }
                }

                return methodNode;
            }
        }
    }
}
