package org.robotframework.ide.eclipse.main.plugin.launch;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.ILaunchConfigurationDelegate;
import org.eclipse.debug.ui.ILaunchShortcut;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.FileEditorInput;
import org.robotframework.ide.core.executor.IRobotOutputListener;
import org.robotframework.ide.core.executor.RobotExecutor;
import org.robotframework.ide.eclipse.main.plugin.launch.tabs.RobotLaunchConfigurationMainTab;

public class RobotLaunchConfigurationDelegate implements ILaunchConfigurationDelegate, ILaunchShortcut {

    private static final String ROBOT_LAUNCH_CONFIGURATION_TYPE = "org.robotframework.ide.robotLaunchConfiguration";

    private static final String ROBOT_CONSOLE_NAME = "Robot Test Result";
    
    private RobotExecutor robotExecutor;

    private ILaunchConfigurationType launchConfigurationType;

    private ILaunchManager manager; 
    
    private IEventBroker broker;

    public RobotLaunchConfigurationDelegate() {
        robotExecutor = new RobotExecutor();
        manager = DebugPlugin.getDefault().getLaunchManager();
        launchConfigurationType = manager.getLaunchConfigurationType(ROBOT_LAUNCH_CONFIGURATION_TYPE);
        broker = (IEventBroker) PlatformUI.getWorkbench().getService(IEventBroker.class);
    }

    @Override
    public void launch(final ILaunchConfiguration configuration, final String mode, final ILaunch launch,
            final IProgressMonitor monitor) throws CoreException {

        String projectNameAttribute = configuration.getAttribute(
                RobotLaunchConfigurationMainTab.PROJECT_NAME_ATTRIBUTE, "");
        IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectNameAttribute);

        if (project.exists()) {
            String fileNameAttribute = configuration.getAttribute(RobotLaunchConfigurationMainTab.FILE_NAME_ATTRIBUTE,
                    "");
            IFile file = project.getFile(fileNameAttribute);
            if (file.exists()) {
                clearMessageLogView();
                String executorNameAttribute = configuration.getAttribute(
                        RobotLaunchConfigurationMainTab.EXECUTOR_NAME_ATTRIBUTE, "");
                String executorArgsAttribute = configuration.getAttribute(
                        RobotLaunchConfigurationMainTab.EXECUTOR_ARGUMENTS_ATTRIBUTE, "");
                executeRobotTest(file, project, executorNameAttribute, executorArgsAttribute);
            }
        }
    }

    @Override
    public void launch(final ISelection selection, final String mode) {
        
        if (selection instanceof IStructuredSelection) {
            WorkspaceJob job = new WorkspaceJob("Launching Robot Tests") {

                @Override
                public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
                    clearMessageLogView();
                    for (Object element : ((IStructuredSelection) selection).toArray()) {
                        if (element instanceof IFile) {
                            IFile file = (IFile) element;
                            launchWithExistingOrNewConfiguration(file, mode, monitor);
                        }
                    }
                    return Status.OK_STATUS;
                }
            };
            job.setUser(false);
            job.schedule();
        }
    }
    
    @Override
    public void launch(IEditorPart editor, final String mode) {
        
        IEditorInput input = editor.getEditorInput();
        if (input instanceof FileEditorInput) {
            final IFile file = ((FileEditorInput) input).getFile();
            WorkspaceJob job = new WorkspaceJob("Launching Robot Tests") {

                @Override
                public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
                    clearMessageLogView();
                    launchWithExistingOrNewConfiguration(file, mode, monitor);

                    return Status.OK_STATUS;
                }
            };
            job.setUser(false);
            job.schedule();
        }
    }

    private void launchWithExistingOrNewConfiguration(IResource file, String mode, IProgressMonitor monitor) {

        if (file instanceof IFile) {

            try {
                ILaunchConfiguration[] configurations = findLaunchConfgurations(file);
                if (configurations.length == 0) {
                    ILaunchConfigurationWorkingCopy configuration = launchConfigurationType.newInstance(null,
                            file.getName());
                    configuration.setAttribute(RobotLaunchConfigurationMainTab.PROJECT_NAME_ATTRIBUTE,
                            file.getProject().getName());
                    configuration.setAttribute(RobotLaunchConfigurationMainTab.FILE_NAME_ATTRIBUTE, file.getParent()
                            .getName() + "/" + file.getName());
                    configuration.setAttribute(RobotLaunchConfigurationMainTab.EXECUTOR_NAME_ATTRIBUTE,
                            RobotLaunchConfigurationMainTab.PYBOT_NAME);

                    configuration.doSave();

                    configurations = new ILaunchConfiguration[] { configuration };

                }

                configurations[0].launch(mode, monitor);

            } catch (CoreException e) {
                e.printStackTrace();
            }
        }
    }

    private ILaunchConfiguration[] findLaunchConfgurations(IResource resource) {

        List<ILaunchConfiguration> configurations = new ArrayList<>();
        try {
            for (ILaunchConfiguration configuration : manager.getLaunchConfigurations(launchConfigurationType)) {
                try {
                    IFile file = getSourceFile(configuration);
                    if (resource.equals(file))
                        configurations.add(configuration);
                } catch (CoreException e) {
                    e.printStackTrace();
                }
            }
        } catch (CoreException e) {
            e.printStackTrace();
        }

        return configurations.toArray(new ILaunchConfiguration[configurations.size()]);
    }

    private IFile getSourceFile(ILaunchConfiguration configuration) throws CoreException {
        String projectName = configuration.getAttribute(RobotLaunchConfigurationMainTab.PROJECT_NAME_ATTRIBUTE, "");
        IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);

        if (project.exists()) {
            String fileName = configuration.getAttribute(RobotLaunchConfigurationMainTab.FILE_NAME_ATTRIBUTE, "");
            IFile file = project.getFile(fileName);

            if (file.exists())
                return file;
        }

        return null;
    }

    private void executeRobotTest(IFile file, IProject project, String executorName, String arguments) {
        ExecutorOutputStreamListener executorOutputStreamListener = new ExecutorOutputStreamListener(ROBOT_CONSOLE_NAME);
        robotExecutor.addOutputStreamListener(executorOutputStreamListener);
        
        robotExecutor.setMessageLogListener(new IRobotOutputListener() {

            @Override
            public void handleLine(String line) {
                broker.send("MessageLogView/AppendLine", line);
            }
        });
        
        robotExecutor.execute(file.getLocation().toPortableString(), project.getLocation().toFile(), executorName,
                arguments);
        robotExecutor.removeOutputStreamListener(executorOutputStreamListener);
    }
    
    private void clearMessageLogView() {
        broker.send("MessageLogView/Clear", "");
    }
}
