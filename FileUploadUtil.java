import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.UUID;

public class FileUploadUtil {
  private static final byte[] HEADER = {0x47, 0x56, 0x41, 0x53};

  public interface FileUploadCallback {
    void onFileUploadSuccess(byte[] data);

    void onFileUploadFailure(String errorMessage);
  }

  public static void useFileUpload(File[] files, FileUploadCallback callback) {
    if (files.length != 1) {
      callback.onFileUploadFailure("Invalid number of files");
      return;
    }

    File file = files[0];

    try (FileInputStream fis = new FileInputStream(file)) {
      byte[] data = new byte[(int) file.length()];
      fis.read(data);

      if (!checkHeader(data)) {
        callback.onFileUploadFailure("Invalid save file");
        return;
      }

      callback.onFileUploadSuccess(data);
    } catch (IOException e) {
      callback.onFileUploadFailure(e.getMessage());
    }
  }

  private static boolean checkHeader(byte[] data) {
    if (data.length < HEADER.length)
      return false;

    for (int i = 0; i < HEADER.length; i++) {
      if (data[i] != HEADER[i])
        return false;
    }

    return true;
  }

  public static void main(String[] args) {
    String filePath = "C:\\Users\\Bruno Fernandes\\Desktop\\drg saves\\76561198046601949_Player.sav";
    File[] files = {new File(filePath)};

    useFileUpload(files, new FileUploadCallback() {
      @Override
      public void onFileUploadSuccess(byte[] data) {
        try {
          String outputFilePath = "C:\\Users\\Bruno Fernandes\\Desktop\\drg saves\\output.txt";
          writeDataToFile(data, outputFilePath);
          int offset = indexOfMulti(data, "Resources".getBytes(), 0);

          ResourceData.Item item = ResourceData.Item.BISMOR;
          int numberOf = getResourcesValue(getUUIDFromByteArray(ResourceData.RESOURCES.get(item)), offset, data);
          System.out.println("Text file created successfully: " + outputFilePath);
          System.out.println("You have " + numberOf + " " + item.name());
        } catch (IOException e) {
          System.out.println("Failed to create text file: " + e.getMessage());
        }
      }

      @Override
      public void onFileUploadFailure(String errorMessage) {
        // Handle file upload failure
        System.out.println("File upload failed: " + errorMessage);
      }
    });
  }

  public static UUID b(String str) {
    long mostSigBits = 0;
    long leastSigBits = 0;

    for (int i = 0; i < str.length(); i++) {
      char c = str.charAt(i);
      if (i < 16) {
        mostSigBits = (mostSigBits << 8) | c;
      } else {
        leastSigBits = (leastSigBits << 8) | c;
      }
    }

    return new UUID(mostSigBits, leastSigBits);
  }

  public static UUID getUUIDFromByteArray(byte[] byteArray) {
    ByteBuffer byteBuffer = ByteBuffer.wrap(byteArray);
    long high = byteBuffer.getLong();
    long low = byteBuffer.getLong();
    return new UUID(high, low);
  }

  public static byte[] getByteArrayFromUUID(UUID uuid) {
    ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
    bb.putLong(uuid.getMostSignificantBits());
    bb.putLong(uuid.getLeastSignificantBits());
    return bb.array();
  }

  public static int getResourcesValue(UUID uuid, int from, byte[] data) {
    float resources = 0;
    resources = getFloat32(uuid, from, data);

    return resources < 0 ? 0 : (int) resources;
  }

  public static float getFloat32(UUID needle, int from, byte[] data) {
    byte[] hexNeedle = getByteArrayFromUUID(needle);

    int index = indexOfMulti(data, hexNeedle, from);
    if (index == -1) {
      return -1;
    }

    byte[] dataAux = Arrays.copyOfRange(data, index + hexNeedle.length, index + hexNeedle.length + 4);
    ByteBuffer buffer = ByteBuffer.wrap(dataAux);
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    return buffer.getFloat();
  }

  private static void writeDataToFile(byte[] data, String filePath) throws IOException {
    Path outputPath = Paths.get(filePath);
    try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath.toFile()))) {
      writer.write(new String(data));
    }
  }

  public static int indexOfMulti(byte[] data, byte[] needle, int offset) {
    byte firstByte = needle[0];
    int index = indexOf(data, firstByte, offset);

    if (needle.length == 1 || index == -1) {
      return index;
    }

    int currentIndex = 0;
    int temporaryIndex = index;

    for (; temporaryIndex < data.length; temporaryIndex++) {
      if (data[temporaryIndex] == needle[currentIndex]) {
        currentIndex += 1;
        index = temporaryIndex;
        if (currentIndex == needle.length) {
          return index - needle.length + 1;
        }
      } else {
        currentIndex = 1;
        temporaryIndex = indexOf(data, needle[0], temporaryIndex);
        if (temporaryIndex == -1) {
          return -1;
        }
      }
    }

    return temporaryIndex == index + needle.length ? index : -1;
  }

  public static int indexOf(byte[] array, byte target, int fromIndex) {
    for (int i = fromIndex; i < array.length; i++) {
      if (array[i] == target) {
        return i;
      }
    }
    return -1;
  }
}
