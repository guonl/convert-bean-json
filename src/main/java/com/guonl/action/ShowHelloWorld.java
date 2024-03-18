package com.guonl.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;

public class ShowHelloWorld extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        // 获取项目信息,就是当前打开的项目信息，弹窗需要使用
        Project project = e.getData(PlatformDataKeys.PROJECT);
        // 展示输入框，提示输入名称，并获取名称
        String username= Messages.showInputDialog(project, "你的名称是？", "输入你的名称", Messages.getQuestionIcon());
        // 获取并展示欢迎语
        Messages.showMessageDialog(project, "Hi, " + username+ "!\n Hello world.", "Information", Messages.getInformationIcon());
    }
}
