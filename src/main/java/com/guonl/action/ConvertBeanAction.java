package com.guonl.action;

import com.intellij.notification.*;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.util.ArrayList;
import java.util.List;

public class ConvertBeanAction extends AnAction {

    //private static final NotificationGroup NOTIFICATION_GROUP = new NotificationGroup("ConvertBean.NotificationGroup", NotificationDisplayType.BALLOON, true);
    private static final NotificationGroup NOTIFICATION_GROUP = NotificationGroupManager.getInstance().getNotificationGroup("ConvertBean.NotificationGroup");

    private Project project;

    private PsiClass selectedClass;

    @Override
    public void actionPerformed(AnActionEvent e) {
        project = e.getData(PlatformDataKeys.PROJECT);
        PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
        Editor editor = e.getData(CommonDataKeys.EDITOR);

        if (editor == null || project == null || psiFile == null) {
            return;
        }

        selectedClass = getTargetClass(editor, psiFile);

        if (selectedClass == null) {
            Notification error = NOTIFICATION_GROUP.createNotification("请在java的类文件内使用！", NotificationType.ERROR);
            Notifications.Bus.notify(error, project);
            return;
        }

        try {
            String content = getContent();

            //将内容放到剪贴板上
            StringSelection selection = new StringSelection(content);
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(selection, selection);
            String message = selectedClass.getName() + " Convert方法已生成，快去粘贴吧~";
            Notification success = NOTIFICATION_GROUP.createNotification(message, NotificationType.INFORMATION);
            Notifications.Bus.notify(success, project);
        } catch (Exception ex) {
            Notification error = NOTIFICATION_GROUP.createNotification("生成失败！", NotificationType.ERROR);
            Notifications.Bus.notify(error, project);
        }

    }

    private String getContent() throws Exception {
        StringBuilder builder = new StringBuilder();
        List<String> fieldList = getFieldName(selectedClass);
        String className = selectedClass.getName();

        builder.append("private " + className + " convert2" + className + "(" + className + " source" + ") {");
        builder.append("\r\n");
        builder.append("if (source == null) {\n" +
                "            return null;\n" +
                "        }");
        builder.append("\r\n");
        builder.append(className + " target = new " + className + "();" + "\r\n");
        for (String field : fieldList) {
            builder.append("target." + getWriteMethod(field) + "(source." + getReadMethod(field) + "());" + "\r\n");
        }

        builder.append("return target;" + "\r\n");
        builder.append("}");
        return builder.toString();
    }

    private String getReadMethod(String fieldName) throws Exception {
        StringBuilder builder = new StringBuilder();
        builder.append("get");
        builder.append(fieldName.substring(0, 1).toUpperCase());
        builder.append(fieldName.substring(1));
        return builder.toString();
    }

    private String getWriteMethod(String fieldName) throws Exception {
        StringBuilder builder = new StringBuilder();
        builder.append("set");
        builder.append(fieldName.substring(0, 1).toUpperCase());
        builder.append(fieldName.substring(1));
        return builder.toString();
    }


    private void allertMessage(String title, String message) {
        Messages.showMessageDialog(project, message, title, Messages.getInformationIcon());
    }


    /**
     * 获取目标类
     *
     * @param editor
     * @param file
     * @return
     */
    private PsiClass getTargetClass(Editor editor, PsiFile file) {
        int offset = editor.getCaretModel().getOffset();
        PsiElement element = file.findElementAt(offset);
        if (element != null) {
            // 当前类
            PsiClass target = PsiTreeUtil.getParentOfType(element, PsiClass.class);

            return target instanceof SyntheticElement ? null : target;
        }
        return null;
    }

    private List<String> getFieldName(PsiClass psiClass) {
        List<String> fieldList = new ArrayList<>();

        if (psiClass != null && !psiClass.isEnum() && !psiClass.isInterface() && !psiClass.isAnnotationType()) {
            for (PsiField field : psiClass.getAllFields()) {
                PsiType type = field.getType();
                String name = field.getName();
                fieldList.add(name);
            }
        }
        return fieldList;
    }

}
