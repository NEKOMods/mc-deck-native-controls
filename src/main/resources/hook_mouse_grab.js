function initializeCoreMod() {
    return {
        'hook_mouse_grab': {
            'target': {
                'type': 'METHOD',
                'class': 'net.minecraft.client.MouseHandler',
                'methodName': 'm_91601_',
                'methodDesc': '()V'
            },
            'transformer': function(methodNode) {
                var asmapi = Java.type('net.minecraftforge.coremod.api.ASMAPI');

                asmapi.log("INFO", "[Deck Native Controls] MouseHandler.grabMouse transformer");

                var grab_func_call = asmapi.findFirstMethodCall(
                    methodNode,
                    asmapi.MethodType.STATIC,
                    "com/mojang/blaze3d/platform/InputConstants",
                    asmapi.mapMethod("m_84833_"),
                    "(JIDD)V");

                if (grab_func_call === null) {
                    asmapi.log("ERROR", "[Deck Native Controls] couldn't find grabOrReleaseMouse");
                    return methodNode;
                }

                methodNode.instructions.insert(grab_func_call, asmapi.buildMethodCall(
                    "nekomods/deckcontrols/TouchscreenInput",
                    "touchRemoveFromCursor",
                    "()V",
                    asmapi.MethodType.STATIC));

                return methodNode;
            }
        },
        'hook_mouse_ungrab': {
            'target': {
                'type': 'METHOD',
                'class': 'net.minecraft.client.MouseHandler',
                'methodName': 'm_91602_',
                'methodDesc': '()V'
            },
            'transformer': function(methodNode) {
                var asmapi = Java.type('net.minecraftforge.coremod.api.ASMAPI');

                asmapi.log("INFO", "[Deck Native Controls] MouseHandler.releaseMouse transformer");

                var grab_func_call = asmapi.findFirstMethodCall(
                    methodNode,
                    asmapi.MethodType.STATIC,
                    "com/mojang/blaze3d/platform/InputConstants",
                    asmapi.mapMethod("m_84833_"),
                    "(JIDD)V");

                if (grab_func_call === null) {
                    asmapi.log("ERROR", "[Deck Native Controls] couldn't find grabOrReleaseMouse");
                    return methodNode;
                }

                methodNode.instructions.insert(grab_func_call, asmapi.buildMethodCall(
                    "nekomods/deckcontrols/TouchscreenInput",
                    "touchReturnToCursor",
                    "()V",
                    asmapi.MethodType.STATIC));

                return methodNode;
            }
        }
    }
}
