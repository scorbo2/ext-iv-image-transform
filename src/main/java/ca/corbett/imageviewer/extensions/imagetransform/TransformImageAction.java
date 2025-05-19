package ca.corbett.imageviewer.extensions.imagetransform;

import ca.corbett.imageviewer.ui.ImageInstance;
import ca.corbett.imageviewer.ui.MainWindow;

import javax.swing.AbstractAction;
import java.awt.event.ActionEvent;
import java.io.File;

/**
 * Launches the TransformImageDialog for the selected image, if there is one.
 *
 * @author scorbo2
 */
public class TransformImageAction extends AbstractAction {

    public TransformImageAction() {
        super("Transform this image...");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        ImageInstance currentImage = MainWindow.getInstance().getSelectedImage();
        if (currentImage.isEmpty()) {
            MainWindow.getInstance().showMessageDialog("Transform image", "Nothing selected.");
            return;
        }

        // Ensure correct file format:
        File file = currentImage.getImageFile();
        if (!file.getName().toLowerCase().endsWith("jpg")
            && !file.getName().toLowerCase().endsWith("jpeg")
            && !file.getName().toLowerCase().endsWith("png")) {
            MainWindow.getInstance().showMessageDialog("Transform image",
                                                       "Image transformation can currently only be performed on jpeg or png images.");
            return;
        }

        new TransformImageDialog(file).setVisible(true);
    }

}
