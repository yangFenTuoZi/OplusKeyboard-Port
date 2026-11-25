import com.android.tools.smali.dexlib2.DexFileFactory;
import com.android.tools.smali.dexlib2.Opcode;
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation;
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction11x;
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction21t;
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction35c;
import com.android.tools.smali.dexlib2.iface.ClassDef;
import com.android.tools.smali.dexlib2.iface.Method;
import com.android.tools.smali.dexlib2.iface.MethodImplementation;
import com.android.tools.smali.dexlib2.iface.instruction.Instruction;
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction35c;
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction3rc;
import com.android.tools.smali.dexlib2.iface.reference.MethodReference;
import com.android.tools.smali.dexlib2.immutable.ImmutableClassDef;
import com.android.tools.smali.dexlib2.immutable.ImmutableDexFile;
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod;
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodImplementation;
import com.android.tools.smali.dexlib2.immutable.instruction.ImmutableInstruction10x;
import com.android.tools.smali.dexlib2.immutable.instruction.ImmutableInstruction11x;
import com.android.tools.smali.dexlib2.immutable.instruction.ImmutableInstruction11n;
import com.android.tools.smali.dexlib2.immutable.instruction.ImmutableInstruction12x;
import com.android.tools.smali.dexlib2.immutable.instruction.ImmutableInstruction22c;
import com.android.tools.smali.dexlib2.immutable.instruction.ImmutableInstruction22t;
import com.android.tools.smali.dexlib2.immutable.instruction.ImmutableInstruction21c;
import com.android.tools.smali.dexlib2.immutable.instruction.ImmutableInstruction21s;
import com.android.tools.smali.dexlib2.immutable.instruction.ImmutableInstruction35c;
import com.android.tools.smali.dexlib2.immutable.instruction.ImmutableInstruction3rc;
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableFieldReference;
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableMethodReference;
import com.android.tools.smali.dexlib2.writer.io.MemoryDataStore;
import com.android.tools.smali.dexlib2.writer.pool.DexPool;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class PatchDex {
    private static final String KEYBOARD = "Lcom/oplus/keyboard/";
    private static final String COMPAT = KEYBOARD + "compat/AospCompat;";
    private static final String DEVICE = KEYBOARD + "utils/h;";
    private static final String BLUR = KEYBOARD + "base/util/f;";
    private static final String BLUR_CONTAINER = "Lcom/heytap/common/bean/d;";
    private static final String EDITOR_APPS = "Lc/a/c/i/obf/M;";
    private static final String SIGNATURE_READER = "Lcom/heytap/omas/a/e/b;";
    private static final String SOUND = KEYBOARD + "base/util/V;";
    private static final String LINEAR_MOTOR = KEYBOARD + "base/util/C;";
    private static final String SETTINGS_PREFS = KEYBOARD + "base/data/o;";
    private static final String IME = KEYBOARD + "service/ImeService;";
    private static final String APP_SETTINGS = KEYBOARD + "base/data/o;";
    private static final String CONTEXT = "Landroid/content/Context;";
    private static final String VIBRATOR = "Landroid/os/Vibrator;";
    private static final String WINDOW = "Landroid/view/Window;";
    private static final String EDITOR_INFO = "Landroid/view/inputmethod/EditorInfo;";
    private static final String DURATIONS = "[Ljava/lang/Long;";
    private static final String AMPLITUDES = "[Ljava/lang/Integer;";
    private static final String EFFECT_TYPES = "[Ljava/lang/Integer;";

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("用法：PatchDex <原版 APK> <--check 或 DEX 输出目录>");
        }
        boolean checkOnly = args[1].equals("--check");
        var container = DexFileFactory.loadDexContainer(new File(args[0]), null);
        Map<String, Integer> hits = new HashMap<>();
        Map<String, ImmutableDexFile> changedDexFiles = new HashMap<>();

        for (String name : container.getDexEntryNames()) {
            var dex = container.getEntry(name).getDexFile();
            List<ClassDef> classes = new ArrayList<>();
            boolean changed = false;
            for (ClassDef cls : dex.getClasses()) {
                ClassDef result = patchClass(cls, hits);
                classes.add(result);
                changed |= result != cls;
            }
            if (changed) {
                changedDexFiles.put(name, new ImmutableDexFile(dex.getOpcodes(), classes));
            }
        }

        Map<String, Integer> expected = new HashMap<>(Map.of(
                "设备形态", 1, "按键触感", 1, "导航栏", 1, "内测资格", 1,
                "触感反馈", 31, "触感开关", 2, "键盘模糊开关", 1, "键盘模糊平台条件", 1,
                "系统设置搜索", 1, "键盘模糊描边", 1));
        expected.put("白盒证书读取", 1);
        expected.put("语音触感", 1);
        if (!hits.equals(expected)) {
            throw new IllegalStateException("补丁位置与预期不符。实际 " + hits + "，预期 " + expected);
        }
        for (var entry : changedDexFiles.entrySet()) {
            if (checkOnly) {
                DexPool.writeTo(new MemoryDataStore(), entry.getValue());
            } else {
                String output = Path.of(args[1], entry.getKey()).toString();
                DexFileFactory.writeDexFile(output, entry.getValue());
            }
            System.out.println("已核对 " + entry.getKey());
        }
        System.out.println(hits);
    }

    private static ClassDef patchClass(ClassDef cls, Map<String, Integer> hits) {
        String owner = cls.getType();
        boolean changed = false;
        List<Method> methods = new ArrayList<>();
        for (Method method : cls.getMethods()) {
            MethodImplementation original = method.getImplementation();
            MethodImplementation replacement = original;
            if (owner.equals(APP_SETTINGS) && method.equals(ref(APP_SETTINGS, "l0", "Z"))) {
                replacement = body(1,
                        new ImmutableInstruction11n(Opcode.CONST_4, 0, 1),
                        new ImmutableInstruction11x(Opcode.RETURN, 0));
                hits.merge("内测资格", 1, Integer::sum);
            } else if (owner.equals(DEVICE) && method.equals(ref(DEVICE, "r", "Z"))) {
                replacement = body(3,
                        call(ref(KEYBOARD + "b;", "a", CONTEXT), 0, 0, 0, 0, 0),
                        new ImmutableInstruction11x(Opcode.MOVE_RESULT_OBJECT, 0),
                        new ImmutableInstruction35c(Opcode.INVOKE_VIRTUAL, 1, 0, 0, 0, 0, 0,
                                ref(CONTEXT, "getResources", "Landroid/content/res/Resources;")),
                        new ImmutableInstruction11x(Opcode.MOVE_RESULT_OBJECT, 1),
                        new ImmutableInstruction35c(Opcode.INVOKE_VIRTUAL, 1, 1, 0, 0, 0, 0,
                                ref("Landroid/content/res/Resources;", "getConfiguration",
                                        "Landroid/content/res/Configuration;")),
                        new ImmutableInstruction11x(Opcode.MOVE_RESULT_OBJECT, 1),
                        new ImmutableInstruction22c(Opcode.IGET, 1, 1,
                                new ImmutableFieldReference(
                                        "Landroid/content/res/Configuration;",
                                        "smallestScreenWidthDp", "I")),
                        new ImmutableInstruction21s(Opcode.CONST_16, 2, 600),
                        new ImmutableInstruction22t(Opcode.IF_GE, 1, 2, 4),
                        new ImmutableInstruction11n(Opcode.CONST_4, 0, 0),
                        new ImmutableInstruction11x(Opcode.RETURN, 0),
                        new ImmutableInstruction11n(Opcode.CONST_4, 0, 1),
                        new ImmutableInstruction11x(Opcode.RETURN, 0));
                hits.merge("设备形态", 1, Integer::sum);
            } else if (owner.equals(SOUND) && method.equals(ref(SOUND, "d", "V", "I"))) {
                replacement = body(9, soundField(0, "h", VIBRATOR),
                        new ImmutableInstruction12x(Opcode.MOVE, 1, 8),
                        soundField(2, "a", DURATIONS), soundField(3, "b", AMPLITUDES),
                        soundField(4, "d", EFFECT_TYPES),
                        new ImmutableInstruction3rc(Opcode.INVOKE_STATIC_RANGE, 0, 5,
                                ref(COMPAT, "vibrateKey", "V", VIBRATOR, "I", DURATIONS,
                                        AMPLITUDES, EFFECT_TYPES)),
                        new ImmutableInstruction10x(Opcode.RETURN_VOID));
                hits.merge("按键触感", 1, Integer::sum);
            } else if (owner.equals(LINEAR_MOTOR)
                    && method.equals(ref(LINEAR_MOTOR, "a", "V", "I"))) {
                replacement = body(2,
                        call(ref(KEYBOARD + "b;", "a", CONTEXT), 0, 0, 0, 0, 0),
                        new ImmutableInstruction11x(Opcode.MOVE_RESULT_OBJECT, 0),
                        call(ref(COMPAT, "vibrateVoiceFeedback", "V", CONTEXT),
                                1, 0, 0, 0, 0),
                        new ImmutableInstruction10x(Opcode.RETURN_VOID));
                hits.merge("语音触感", 1, Integer::sum);
            } else if (owner.equals(BLUR) && method.equals(ref(BLUR, "b", "Z"))) {
                replacement = patchBlurPlatformCheck(original);
                hits.merge("键盘模糊平台条件", 1, Integer::sum);
            } else if (owner.equals(BLUR_CONTAINER) && method.equals(ref(BLUR_CONTAINER, "a", "V"))) {
                replacement = patchKeyboardEdge(original, hits);
            } else if (owner.equals(EDITOR_APPS)
                    && method.equals(ref(EDITOR_APPS, "K", "Z", EDITOR_INFO))) {
                replacement = patchSettingsSearchEditor(original);
                hits.merge("系统设置搜索", 1, Integer::sum);
            } else if (owner.equals(SIGNATURE_READER)
                    && method.equals(ref(SIGNATURE_READER, "b", "Ljava/lang/String;",
                    CONTEXT, "Ljava/lang/String;"))) {
                replacement = patchSignatureReader(original, hits);
            } else if (owner.equals(SETTINGS_PREFS)
                    && method.equals(ref(SETTINGS_PREFS, "j", "Z"))) {
                replacement = body(1,
                        call(ref(KEYBOARD + "b;", "a", CONTEXT), 0, 0, 0, 0, 0),
                        new ImmutableInstruction11x(Opcode.MOVE_RESULT_OBJECT, 0),
                        call(ref(COMPAT, "isKeyboardBlurEnabled", "Z", CONTEXT), 1, 0, 0, 0, 0),
                        new ImmutableInstruction11x(Opcode.MOVE_RESULT, 0),
                        new ImmutableInstruction11x(Opcode.RETURN, 0));
                hits.merge("键盘模糊开关", 1, Integer::sum);
            } else if (owner.equals(SETTINGS_PREFS)
                    && method.equals(ref(SETTINGS_PREFS, "i0", "Z"))) {
                replacement = body(1,
                        call(ref(KEYBOARD + "b;", "a", CONTEXT), 0, 0, 0, 0, 0),
                        new ImmutableInstruction11x(Opcode.MOVE_RESULT_OBJECT, 0),
                        call(ref(COMPAT, "isKeyVibrationEnabled", "Z", CONTEXT),
                                1, 0, 0, 0, 0),
                        new ImmutableInstruction11x(Opcode.MOVE_RESULT, 0),
                        new ImmutableInstruction11x(Opcode.RETURN, 0));
                hits.merge("触感开关", 1, Integer::sum);
            } else if (owner.equals(SETTINGS_PREFS)
                    && method.equals(ref(SETTINGS_PREFS, "h1", "V", "Z"))) {
                replacement = body(2,
                        call(ref(KEYBOARD + "b;", "a", CONTEXT), 0, 0, 0, 0, 0),
                        new ImmutableInstruction11x(Opcode.MOVE_RESULT_OBJECT, 0),
                        call(ref(COMPAT, "setKeyVibrationEnabled", "V", CONTEXT, "Z"),
                                2, 0, 1, 0, 0),
                        new ImmutableInstruction10x(Opcode.RETURN_VOID));
                hits.merge("触感开关", 1, Integer::sum);
            } else if (owner.equals(IME) && method.equals(ref(IME, "d", "V", WINDOW, "I"))) {
                replacement = patchNavigationBar(original);
                hits.merge("导航栏", 1, Integer::sum);
            }
            if (replacement == original && original != null) {
                replacement = patchHapticFeedback(original, hits);
            }
            if (replacement == original) {
                methods.add(method);
            } else {
                changed = true;
                methods.add(new ImmutableMethod(owner, method.getName(), method.getParameters(),
                        method.getReturnType(), method.getAccessFlags(), method.getAnnotations(),
                        method.getHiddenApiRestrictions(), replacement));
            }
        }
        if (!changed) {
            return cls;
        }
        return new ImmutableClassDef(owner, cls.getAccessFlags(), cls.getSuperclass(),
                cls.getInterfaces(), cls.getSourceFile(), cls.getAnnotations(), cls.getFields(),
                methods);
    }

    private static MethodImplementation patchSignatureReader(MethodImplementation original,
                                                               Map<String, Integer> hits) {
        var originalReader = ref(SIGNATURE_READER, "a", "[Landroid/content/pm/Signature;",
                CONTEXT, "Ljava/lang/String;");
        var certificateReader = ref(KEYBOARD + "compat/OriginalCertificate;", "getSignatures",
                "[Landroid/content/pm/Signature;", CONTEXT, "Ljava/lang/String;");
        List<Instruction> instructions = new ArrayList<>();
        for (Instruction instruction : original.getInstructions()) {
            if (instruction.getOpcode() == Opcode.INVOKE_STATIC
                    && instruction instanceof Instruction35c invoke
                    && originalReader.equals(invoke.getReference())) {
                instruction = new ImmutableInstruction35c(Opcode.INVOKE_STATIC,
                        invoke.getRegisterCount(), invoke.getRegisterC(), invoke.getRegisterD(),
                        invoke.getRegisterE(), invoke.getRegisterF(), invoke.getRegisterG(),
                        certificateReader);
                hits.merge("白盒证书读取", 1, Integer::sum);
            }
            instructions.add(instruction);
        }
        return new ImmutableMethodImplementation(original.getRegisterCount(), instructions,
                original.getTryBlocks(), original.getDebugItems());
    }

    private static MethodImplementation patchHapticFeedback(MethodImplementation original,
                                                            Map<String, Integer> hits) {
        List<Instruction> instructions = new ArrayList<>();
        boolean changed = false;
        for (Instruction instruction : original.getInstructions()) {
            if (instruction.getOpcode() == Opcode.INVOKE_VIRTUAL
                    && instruction instanceof Instruction35c call
                    && call.getReference() instanceof MethodReference reference
                    && reference.getName().equals("performHapticFeedback")) {
                ImmutableMethodReference replacement = null;
                if (reference.getParameterTypes().equals(List.of("I"))
                        && reference.getReturnType().equals("Z")) {
                    replacement = ref(COMPAT, "performHapticFeedback", "Z",
                            "Landroid/view/View;", "I");
                } else if (reference.getParameterTypes().equals(List.of("I", "I"))
                        && reference.getReturnType().equals("Z")) {
                    replacement = ref(COMPAT, "performHapticFeedback", "Z",
                            "Landroid/view/View;", "I", "I");
                }
                if (replacement != null) {
                    instruction = new ImmutableInstruction35c(Opcode.INVOKE_STATIC,
                            call.getRegisterCount(), call.getRegisterC(), call.getRegisterD(),
                            call.getRegisterE(), call.getRegisterF(), 0, replacement);
                    changed = true;
                    hits.merge("触感反馈", 1, Integer::sum);
                }
            }
            instructions.add(instruction);
        }
        if (!changed) {
            return original;
        }
        return new ImmutableMethodImplementation(original.getRegisterCount(), instructions,
                original.getTryBlocks(), original.getDebugItems());
    }

    private static MethodImplementation patchNavigationBar(MethodImplementation original) {
        // 保留主题和背景色决定图标明暗的原始分支。所有分支在此处汇合。
        int appearanceOffset = 0x19;
        int offset = 0;
        List<Instruction> instructions = new ArrayList<>();
        for (Instruction instruction : original.getInstructions()) {
            if (offset >= appearanceOffset) {
                if (offset != appearanceOffset || original.getRegisterCount() != 11
                        || instruction.getOpcode() != Opcode.IF_NEZ) {
                    throw new IllegalStateException("导航栏方法的指令布局发生变化");
                }
                instructions.add(call(ref(COMPAT, "applyNavigationBar", "V", WINDOW, "I", "Z"),
                        3, 9, 10, 2, 0));
                instructions.add(new ImmutableInstruction10x(Opcode.RETURN_VOID));
                return new ImmutableMethodImplementation(11, instructions, List.of(), List.of());
            }
            instructions.add(instruction);
            offset += instruction.getCodeUnits();
        }
        throw new IllegalStateException("找不到导航栏图标明暗分支的汇合点");
    }

    private static MethodImplementation patchBlurPlatformCheck(MethodImplementation original) {
        // 设备条件已由 isKeyboardBlurEnabled 检查。保留键盘状态和主题判断。
        int stateOffset = 0x14;
        int offset = 0;
        List<Instruction> instructions = new ArrayList<>();
        instructions.add(new ImmutableInstruction11n(Opcode.CONST_4, 0, 0));
        instructions.add(new ImmutableInstruction11n(Opcode.CONST_4, 3, 1));
        for (Instruction instruction : original.getInstructions()) {
            if (offset >= stateOffset) {
                instructions.add(instruction);
            }
            offset += instruction.getCodeUnits();
        }
        if (original.getRegisterCount() != 4 || offset != 0x2e || instructions.size() <= 2
                || instructions.get(2).getOpcode() != Opcode.SGET_OBJECT) {
            throw new IllegalStateException("键盘模糊条件的指令布局发生变化");
        }
        return new ImmutableMethodImplementation(4, instructions, List.of(), List.of());
    }

    private static MethodImplementation patchSettingsSearchEditor(MethodImplementation original) {
        if (original.getRegisterCount() != 6) {
            throw new IllegalStateException("模糊应用列表的寄存器布局发生变化");
        }
        var implementation = new MutableMethodImplementation(original);
        var originalStart = implementation.newLabelForIndex(0);
        implementation.addInstruction(0, new BuilderInstruction35c(Opcode.INVOKE_STATIC,
                1, 5, 0, 0, 0, 0, ref(COMPAT, "isSettingsSearchEditor", "Z", EDITOR_INFO)));
        implementation.addInstruction(1, new BuilderInstruction11x(Opcode.MOVE_RESULT, 0));
        implementation.addInstruction(2, new BuilderInstruction21t(Opcode.IF_EQZ, 0, originalStart));
        implementation.addInstruction(3, new BuilderInstruction11x(Opcode.RETURN, 0));
        return implementation;
    }

    private static MethodImplementation patchKeyboardEdge(MethodImplementation original,
                                                          Map<String, Integer> hits) {
        var materialCheck = ref("Lcom/coui/appcompat/COUIMaterialEffect;", "isMaterialVersion", "Z");
        var shaderCheck = ref("Lcom/coui/appcompat/COUIEffectVersionUtil;", "isRuntimeShaderVersion", "Z");
        List<Instruction> instructions = new ArrayList<>();
        for (Instruction instruction : original.getInstructions()) {
            // 键盘描边使用 RuntimeShader 绘制。这里只替换描边入口的版本判断。
            if (instruction.getOpcode() == Opcode.INVOKE_STATIC
                    && instruction instanceof Instruction35c invoke
                    && materialCheck.equals(invoke.getReference())) {
                instruction = call(shaderCheck, 0, 0, 0, 0, 0);
                hits.merge("键盘模糊描边", 1, Integer::sum);
            }
            instructions.add(instruction);
        }
        return new ImmutableMethodImplementation(original.getRegisterCount(), instructions,
                original.getTryBlocks(), original.getDebugItems());
    }

    private static ImmutableMethodReference ref(String owner, String name, String result,
                                                String... parameters) {
        return new ImmutableMethodReference(owner, name, List.of(parameters), result);
    }

    private static Instruction soundField(int register, String name, String type) {
        return new ImmutableInstruction21c(Opcode.SGET_OBJECT, register,
                new ImmutableFieldReference(SOUND, name, type));
    }

    private static Instruction call(ImmutableMethodReference target, int count,
                                    int c, int d, int e, int f) {
        return new ImmutableInstruction35c(Opcode.INVOKE_STATIC, count, c, d, e, f, 0, target);
    }

    private static MethodImplementation body(int registers, Instruction... instructions) {
        return new ImmutableMethodImplementation(registers, List.of(instructions),
                List.of(), List.of());
    }
}
