package com.guonl.action;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.guonl.utils.GsonFormatUtil;
import com.intellij.notification.*;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.util.InheritanceUtil;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiTypesUtil;
import com.intellij.psi.util.PsiUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.math.BigDecimal;
import java.util.List;
import java.util.*;

public class ConvertJsonAction extends AnAction {

    //private static final NotificationGroup NOTIFICATION_GROUP = new NotificationGroup("Java2Json.NotificationGroup", NotificationDisplayType.BALLOON, true);
    private static final NotificationGroup NOTIFICATION_GROUP = NotificationGroupManager.getInstance().getNotificationGroup("ConvertBean.NotificationGroup");

    @NonNls
    private static final Map<String, Object> PROPERTIES_TYPES = new HashMap<>(16);
    @NonNls
    private static final Set<String> ANNOTATION_TYPES = new HashSet<>();


    static {
        // 包装数据类型
        PROPERTIES_TYPES.put("Byte", 0);
        PROPERTIES_TYPES.put("Short", 0);
        PROPERTIES_TYPES.put("Integer", 0);
        PROPERTIES_TYPES.put("Long", 0L);
        PROPERTIES_TYPES.put("Float", 0.0F);
        PROPERTIES_TYPES.put("Double", 0.0D);
        PROPERTIES_TYPES.put("Boolean", false);
        // 其他
        PROPERTIES_TYPES.put("String", "");
        PROPERTIES_TYPES.put("BigDecimal", BigDecimal.ZERO);
        PROPERTIES_TYPES.put("Date", null);
        PROPERTIES_TYPES.put("LocalDate", null);
        PROPERTIES_TYPES.put("LocalTime", null);
        PROPERTIES_TYPES.put("LocalDateTime", null);

        // 注解过滤
        ANNOTATION_TYPES.add("javax.annotation.Resource");
        ANNOTATION_TYPES.add("org.springframework.beans.factory.annotation.Autowired");
    }

    @Override
    public void actionPerformed(AnActionEvent e) {

        Project project = e.getData(PlatformDataKeys.PROJECT);
        PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
        Editor editor = e.getData(CommonDataKeys.EDITOR);

        if (editor == null || project == null || psiFile == null) {
            return;
        }

        PsiClass selectedClass = getTargetClass(editor, psiFile);

        if (selectedClass == null) {
            Notification error = NOTIFICATION_GROUP.createNotification("请在java的类文件内使用!", NotificationType.ERROR);
            Notifications.Bus.notify(error, project);
            return;
        }

        try {
            Map<String, Object> fieldsMap = getFields(selectedClass);

            Gson gson = new GsonBuilder().serializeNulls().create();
            String json = GsonFormatUtil.gsonFormat(gson, fieldsMap);

            StringSelection selection = new StringSelection(json);
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(selection, selection);
            String message = selectedClass.getName() + "转JSON成功, 快去粘贴吧~";
            Notification success = NOTIFICATION_GROUP.createNotification(message, NotificationType.INFORMATION);
            Notifications.Bus.notify(success, project);
        } catch (Exception ex) {
            Notification error = NOTIFICATION_GROUP.createNotification("转JSON失败！", NotificationType.ERROR);
            Notifications.Bus.notify(error, project);
        }

    }


    /**
     * 获取目标类
     *
     * @param editor
     * @param file
     * @return
     */
    public static PsiClass getTargetClass(@NotNull Editor editor, @NotNull PsiFile file) {
        int offset = editor.getCaretModel().getOffset();
        PsiElement element = file.findElementAt(offset);
        if (element != null) {
            // 当前类
            PsiClass target = PsiTreeUtil.getParentOfType(element, PsiClass.class);

            return target instanceof SyntheticElement ? null : target;
        }
        return null;
    }

    /**
     * 获取类中的字段
     *
     * @param psiClass
     * @return
     */
    public static Map<String, Object> getFields(PsiClass psiClass) {

        Map<String, Object> fieldMap = new LinkedHashMap<>();
        // Map<String, Object> commentFieldMap = new LinkedHashMap<>();

        if (psiClass != null && !psiClass.isEnum() && !psiClass.isInterface() && !psiClass.isAnnotationType()) {
            for (PsiField field : psiClass.getAllFields()) {
                PsiType type = field.getType();
                String name = field.getName();
                // if (field.getDocComment() != null && StringUtils.isNotBlank(field.getDocComment().getText())) {
                //     String fieldComment = field.getDocComment().getText();
                //     commentFieldMap.put(name, CommentUtils.removeSymbol(fieldComment));
                // }
                // 判断注解 javax.annotation.Resource   org.springframework.beans.factory.annotation.Autowired
                PsiAnnotation[] annotations = field.getAnnotations();
                if (annotations.length > 0 && containsAnnotation(annotations)) {
                    fieldMap.put(name, "");
                } else if (type instanceof PsiPrimitiveType) {
                    // 基本类型
                    fieldMap.put(name, PsiTypesUtil.getDefaultValue(type));
                } else {
                    //reference Type
                    String fieldTypeName = type.getPresentableText();
                    // 指定的类型
                    if (PROPERTIES_TYPES.containsKey(fieldTypeName)) {
                        fieldMap.put(name, PROPERTIES_TYPES.get(fieldTypeName));
                    } else if (type instanceof PsiArrayType) {
                        //array type
                        List<Object> list = new ArrayList<>();
                        PsiType deepType = type.getDeepComponentType();
                        String deepTypeName = deepType.getPresentableText();
                        if (deepType instanceof PsiPrimitiveType) {
                            list.add(PsiTypesUtil.getDefaultValue(deepType));
                        } else if (PROPERTIES_TYPES.containsKey(deepTypeName)) {
                            list.add(PROPERTIES_TYPES.get(deepTypeName));
                        } else {
                            list.add(getFields(PsiUtil.resolveClassInType(deepType)));
                        }
                        fieldMap.put(name, list);
                    } else if (InheritanceUtil.isInheritor(type, CommonClassNames.JAVA_UTIL_COLLECTION)) {
                        // List Set or HashSet
                        List<Object> list = new ArrayList<>();
                        PsiType iterableType = PsiUtil.extractIterableTypeParameter(type, false);
                        PsiClass iterableClass = PsiUtil.resolveClassInClassTypeOnly(iterableType);
                        if (iterableClass != null) {
                            String classTypeName = iterableClass.getName();
                            if (PROPERTIES_TYPES.containsKey(classTypeName)) {
                                list.add(PROPERTIES_TYPES.get(classTypeName));
                            } else {
                                list.add(getFields(iterableClass));
                            }
                        }
                        fieldMap.put(name, list);
                    } else if (InheritanceUtil.isInheritor(type, CommonClassNames.JAVA_UTIL_MAP)) {
                        // HashMap or Map
                        fieldMap.put(name, new HashMap<>(4));
                    } else if (psiClass.isEnum() || psiClass.isInterface() || psiClass.isAnnotationType()) {
                        // enum or interface
                        fieldMap.put(name, "");
                    } else {
                        fieldMap.put(name, getFields(PsiUtil.resolveClassInType(type)));
                    }
                }
            }
            // json 串中的注释字段 暂时不添加
            // if (commentFieldMap.size() > 0) {
            //     fieldMap.put("@comment", commentFieldMap);
            // }
        }
        return fieldMap;
    }

    /**
     * 是否包含指定的注解
     *
     * @param annotations
     * @return
     */
    private static boolean containsAnnotation(PsiAnnotation[] annotations) {
        for (PsiAnnotation annotation : annotations) {
            if (ANNOTATION_TYPES.contains(annotation.getQualifiedName())) {
                return true;
            }
        }
        return false;
    }


}
