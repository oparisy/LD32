package org.oparisy.fields.tools.loadbmp;

import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.PixelInterleavedSampleModel;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;

public class SampleModelOrientationTest {
    public static void main(String[] args) {
        BufferedImage image = new BufferedImage(16, 9, BufferedImage.TYPE_3BYTE_BGR);
        WritableRaster raster = image.getRaster();
        DataBuffer dataBuffer = raster.getDataBuffer();

        SampleModel sampleModel = image.getSampleModel();

        QueryingDataBuffer queryBuffer = new QueryingDataBuffer(dataBuffer, sampleModel.getWidth(), sampleModel.getNumDataElements());
        sampleModel.getDataElements(0, 0, null, queryBuffer);
        System.out.println(queryBuffer.getOrientation());

        queryBuffer.resetOrientation();
        SampleModel bottomUpSampleModel = new BottomUpSampleModel(sampleModel);
        bottomUpSampleModel.getDataElements(0, 0, null, queryBuffer);
        System.out.println(queryBuffer.getOrientation());
    }

    private static class QueryingDataBuffer extends DataBuffer {
        enum Orientation {
            Undefined,
            TopDown,
            BottomUp,
            Unsupported
        }

        private final int width;
        private final int numDataElements;

        private Orientation orientation = Orientation.Undefined;

        public QueryingDataBuffer(final DataBuffer dataBuffer, final int width, final int numDataElements) {
            super(dataBuffer.getDataType(), dataBuffer.getSize());
            this.width = width;
            this.numDataElements = numDataElements;
        }

        @Override public int getElem(final int bank, final int i) {
            if (bank == 0 && i < numDataElements && isOrientationUndefinedOrEqualTo(Orientation.TopDown)) {
                orientation = Orientation.TopDown;
            }
            else if (bank == 0 && i >= (size - (width * numDataElements) - numDataElements) && isOrientationUndefinedOrEqualTo(Orientation.BottomUp)) {
                orientation = Orientation.BottomUp;
            }
            else {
                // TODO: Expand with more options as apropriate
                orientation = Orientation.Unsupported;
            }

            return 0;
        }

        private boolean isOrientationUndefinedOrEqualTo(final Orientation orientation) {
            return this.orientation == Orientation.Undefined || this.orientation == orientation;
        }

        @Override public void setElem(final int bank, final int i, final int val) {
        }

        public final void resetOrientation() {
            orientation = Orientation.Undefined;
        }

        public final Orientation getOrientation() {
            return orientation;
        }
    }

    // TODO: This has to be generalized to be used for any BufferedImage type.
    // I justy happen to know that 3BYTE_BGR uses PixelInterleavedSampleModel and has BGR order.
    private static class BottomUpSampleModel extends PixelInterleavedSampleModel {
        public BottomUpSampleModel(final SampleModel sampleModel) {
            super(sampleModel.getDataType(), sampleModel.getWidth(), sampleModel.getHeight(),
                  sampleModel.getNumDataElements(), sampleModel.getNumDataElements() * sampleModel.getWidth(),
                  new int[] {2, 1, 0} // B, G, R
            );
        }

        @Override public Object getDataElements(final int x, final int y, final Object obj, final DataBuffer data) {
            return super.getDataElements(x, getHeight() - 1 - y, obj, data);
        }

        @Override public int getSample(final int x, final int y, final int b, final DataBuffer data) {
            return super.getSample(x, getHeight() - 1 - y, b, data);
        }
    }
}