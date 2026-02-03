package ca.corbett.imageviewer.extensions.imagetransform;

import ca.corbett.extensions.AppExtensionInfo;
import ca.corbett.extras.EnhancedAction;
import ca.corbett.extras.io.KeyStrokeManager;
import ca.corbett.extras.properties.AbstractProperty;
import ca.corbett.extras.properties.KeyStrokeProperty;
import ca.corbett.imageviewer.AppConfig;
import ca.corbett.imageviewer.extensions.ImageViewerExtension;
import ca.corbett.imageviewer.ui.MainWindow;
import ca.corbett.imageviewer.ui.ReservedKeyStrokeWorkaround;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * An ImageViewer extension that adds very basic image transformation capabilities.
 * The selected image can be rotated or flipped, and then saved in place.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 * @since ImageViewer 1.3 originally, moved to extension in ImageViewer 2.0
 */
public class TransformImageExtension extends ImageViewerExtension {

    private static final String keystrokeProp = AppConfig.KEYSTROKE_MISC_PREFIX + "imageTransform";
    private final AppExtensionInfo extInfo;

    public TransformImageExtension() {
        extInfo = AppExtensionInfo.fromExtensionJar(getClass(),
            "/ca/corbett/imageviewer/extensions/imagetransform/extInfo.json");
        if (extInfo == null) {
            throw new RuntimeException("ImageTransformExtension: can't parse extInfo.json!");
        }
    }

    @Override
    public AppExtensionInfo getInfo() {
        return extInfo;
    }

    @Override
    public void loadJarResources() {
    }

    @Override
    protected List<AbstractProperty> createConfigProperties() {
        List<AbstractProperty> props = new ArrayList<>();

        props.add(new KeyStrokeProperty(keystrokeProp,
                                        "Transform image:",
                                        KeyStrokeManager.parseKeyStroke("Shift+T"),
                                        TransformImageAction.getInstance())
                      .setAllowBlank(true)
                      .setHelpText("Show the image transform dialog")
                      .addFormFieldGenerationListener(new ReservedKeyStrokeWorkaround()));

        return props;
    }

    @Override
    public List<EnhancedAction> getMenuActions(String topLevelMenu, MainWindow.BrowseMode browseMode) {
        if ("Edit".equals(topLevelMenu)) {
            return List.of(TransformImageAction.getInstance());
        }
        return null;
    }

    @Override
    public List<EnhancedAction> getPopupMenuActions(MainWindow.BrowseMode browseMode) {
        return List.of(TransformImageAction.getInstance());
    }

    /**
     * Provides a case-insensitive check to see if the given file has a supported image extension.
     */
    public static boolean fileExtensionIsSupported(File f) {
        String name = f.getName().toLowerCase();
        return name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png");
    }
}
