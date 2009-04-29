package com.jetbrains.python.facet;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.impl.LaterInvocator;
import com.intellij.openapi.components.ApplicationComponent;
import com.intellij.openapi.projectRoots.ProjectJdkTable;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryTable;
import com.intellij.openapi.roots.libraries.LibraryTablesRegistrar;
import com.jetbrains.python.sdk.PythonSdkType;
import org.jetbrains.annotations.NotNull;

/**
 * @author yole
 */
public class PythonSdkTableListener implements ApplicationComponent {
  private final ProjectJdkTable.Listener myJdkTableListener;

  public PythonSdkTableListener() {
    myJdkTableListener = new ProjectJdkTable.Listener() {
      public void jdkAdded(final Sdk sdk) {
        if (sdk.getSdkType() instanceof PythonSdkType) {
          addLibrary(sdk);
        }
      }
      public void jdkRemoved(final Sdk sdk) {
        if (sdk.getSdkType() instanceof PythonSdkType) {
          removeLibrary(sdk);
        }
      }
      public void jdkNameChanged(final Sdk sdk, final String previousName) {
        if (sdk.getSdkType() instanceof PythonSdkType) {
          renameLibrary(sdk, previousName);
        }
      }
    };
  }

  static Library addLibrary(Sdk sdk) {
    final LibraryTable.ModifiableModel libraryTableModel = LibraryTablesRegistrar.getInstance().getLibraryTable().getModifiableModel();
    final Library library = libraryTableModel.createLibrary(PythonFacet.getFacetLibraryName(sdk.getName()));
    final Library.ModifiableModel model = library.getModifiableModel();
    for (String url : sdk.getRootProvider().getUrls(OrderRootType.CLASSES)) {
      model.addRoot(url, OrderRootType.CLASSES);
      model.addRoot(url, OrderRootType.SOURCES);
    }
    model.commit();
    libraryTableModel.commit();
    return library;
  }

  private static void removeLibrary(final Sdk sdk) {
    LaterInvocator.invokeLater(new Runnable() {
      public void run() {
        ApplicationManager.getApplication().runWriteAction(new Runnable() {
          public void run()  {
            final LibraryTable.ModifiableModel libraryTableModel = LibraryTablesRegistrar.getInstance().getLibraryTable().getModifiableModel();
            final Library library = libraryTableModel.getLibraryByName(PythonFacet.getFacetLibraryName(sdk.getName()));
            if (library!=null) {
              libraryTableModel.removeLibrary(library);
            }
            libraryTableModel.commit();
          }
        });
      }
    }, ModalityState.NON_MODAL);
  }

  private static void renameLibrary(final Sdk sdk, final String previousName) {
    LaterInvocator.invokeLater(new Runnable() {
      public void run() {
        ApplicationManager.getApplication().runWriteAction(new Runnable() {
          public void run() {
            final LibraryTable.ModifiableModel libraryTableModel = LibraryTablesRegistrar.getInstance().getLibraryTable().getModifiableModel();
            final Library library = libraryTableModel.getLibraryByName(PythonFacet.getFacetLibraryName(previousName));
            if (library!=null){
              final Library.ModifiableModel model = library.getModifiableModel();
              model.setName(PythonFacet.getFacetLibraryName(sdk.getName()));
              model.commit();
            }
            libraryTableModel.commit();
          }
        });
      }
    }, ModalityState.NON_MODAL);
  }

  @NotNull
  public String getComponentName() {
    return "PythonSdkTableListener";
  }

  public void initComponent() {
    ProjectJdkTable.getInstance().addListener(myJdkTableListener);
  }

  public void disposeComponent() {
    ProjectJdkTable.getInstance().removeListener(myJdkTableListener);
  }
}
