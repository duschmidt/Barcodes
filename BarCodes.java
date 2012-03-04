import java.util.HashMap;
import java.util.Map;
import java.util.Collections;
import java.util.Arrays;

class BarCodes {
    /*
      Wrapper class to hold arrays of bytes
      To be used as map keys (since arrays use object equality
      for comparison)
    */
    private static class Encoding {
        public byte[] bytes;

        public Encoding() {
            bytes = new byte[5];
        }
        
        public Encoding(byte[] bytes) {
            this.bytes = bytes;
        }

        public boolean equals(Object aThat) {
            if (this == aThat) return true;
            if (!(aThat instanceof Encoding)) return false;
            Encoding that = (Encoding)aThat;

            return Arrays.equals(this.bytes, that.bytes);
        }

        public int hashCode() {
            return Arrays.hashCode(bytes);
        }
    }

    // initialize the encoding->character map
    public static final Map<Encoding, Character> encodings;
    static {
        Map<Encoding, Character> map = new HashMap<Encoding, Character>();
        map.put(new Encoding(new byte[] {0,0,0,0,1}), '0');
        map.put(new Encoding(new byte[] {1,0,0,0,1}), '1');
        map.put(new Encoding(new byte[] {0,1,0,0,1}), '2');
        map.put(new Encoding(new byte[] {1,1,0,0,0}), '3');
        map.put(new Encoding(new byte[] {0,0,1,0,1}), '4');
        map.put(new Encoding(new byte[] {1,0,1,0,0}), '5');
        map.put(new Encoding(new byte[] {0,1,1,0,0}), '6');
        map.put(new Encoding(new byte[] {0,0,0,1,1}), '7');
        map.put(new Encoding(new byte[] {1,0,0,1,0}), '8');
        map.put(new Encoding(new byte[] {1,0,0,0,0}), '9');
        map.put(new Encoding(new byte[] {0,0,1,0,0}), '-');
        map.put(new Encoding(new byte[] {0,0,1,1,0}), 's');
        encodings = Collections.unmodifiableMap(map);
    }
    
    private static int[] argsToInputs(String[] args) {
        int numInputs = Integer.valueOf(args[0]);
        String[] strInputs = Arrays.copyOfRange(args, 1, args.length);
        int[] intInputs = convertToIntArray(strInputs);
        if (numInputs != intInputs.length) return null;
        if (!checkRange(intInputs)) return null;
        return intInputs;
    }

    /*
      Returns false if any of the provided input elements are
      outside of the range [1, 200].  Else returns true.
    */
    private static boolean checkRange(int[] inputs) {
        for (int i = 0; i < inputs.length; i++) {
            if (inputs[i] < 1 || inputs[i] > 200) {
                return false;
            }
        }
        return true;
    }
    
    /*
      convert an the original input ints to an array of 0's and 1's
    */
    private static byte[] convertToByteEncoding(int[] intInputs) {
        int[] ranges = getRanges(intInputs);
        // TODO: return null if either low range or high range deviates
        //       from their center by more than 5%
        byte[] byteEncoded = new byte[intInputs.length];
        for (int i = 0; i < intInputs.length; i++) {
            if (intInputs[i] >= ranges[0] &&
                intInputs[i] <= ranges[1]) {
                byteEncoded[i] = 0; // narrow bar
            } else if (intInputs[i] >= ranges[2] &&
                       intInputs[i] <= ranges[3]) {
                byteEncoded[i] = 1; // wide bar
            } else {
                System.out.println("Oops.  something wrong with range function!");
                System.exit(2);
            }
        }
        return byteEncoded;
    }

    /*
      convert the given bytes to an array of the characters it encodes.
      bytes arg is assumed to be in the correct order, and
      composed only of character and separator bar encodings.
      (start/stop encodings should be stripped)
    */
    static char[] convertToCharacters(byte[] byteInputs) {
        char[] characters = new char[byteInputs.length/6 + 1];
        Encoding encoding = new Encoding();
        for (int i = 0, j = 0; i < byteInputs.length; i += 6, j++) {
            encoding.bytes = Arrays.copyOfRange(byteInputs, i, i + 5);
            if (encodings.containsKey(encoding)) {
                characters[j] = encodings.get(encoding);
            } else {
                // no character for this encoding
                return null;
            }
        }
        return characters;
    }

    /*
      If the beginning and end aren't the start and stop characters,
      reverse the bytes argument in place, and check again.
      If the start/stop is still not correct, Return null
      If start/stop is correct, Return the byte input arg with the
      start and stop encodings trimmed.
    */
    private static byte[] trimStartStop(byte[] byteInputs) {
        if (!checkStartStop(byteInputs)) {
            byteInputs = getReversedBytes(byteInputs);
            if (!checkStartStop(byteInputs)) {
                return null;
            }
        }
        // trim the start/stop bytes and the separators next to them
        return Arrays.copyOfRange(byteInputs, 6, byteInputs.length - 6);
    }

    /*
      Return true if the first and last 5 bytes of the input encode
      for the start and stop characters respectively, else Return false
    */
    private static boolean checkStartStop(byte[] byteInputs) {
        byte[] start = Arrays.copyOfRange(byteInputs, 0, 5);
        byte[] stop = Arrays.copyOfRange(byteInputs, byteInputs.length - 5,
                                         byteInputs.length);
        Encoding startEncoding = new Encoding(start);        
        Encoding stopEncoding = new Encoding(stop);
        return (encodings.containsKey(startEncoding) &&
                encodings.get(startEncoding) == 's' &&
                encodings.containsKey(stopEncoding) &&
                encodings.get(stopEncoding) == 's');
    }

    private static byte[] getReversedBytes(byte[] bytes) {
        byte[] reversed = new byte[bytes.length];
        for (int i = 0, j = bytes.length - 1; i < bytes.length; i++, j--) {
            reversed[i] = bytes[j];
        }
        return reversed;
    }
    
    /*
      Returns an array of four ints, with the format
      [narrowMin, narrowMax, wideMin, highMax],
      where [narrowMin, narrowMax] is the range of the narrow values,
      and [wideMin, wideMax] is the range of the wide values
      These are calculated by sorting the inputs and finding the
      largest gap between adjacent elements, which is the gap between
      narrowMax and wideMin
    */
    private static int[] getRanges(int[] intInputs) {
        if (intInputs == null || intInputs.length == 0) return null;
        int[] sortedCpy = Arrays.copyOf(intInputs, intInputs.length);
        Arrays.sort(sortedCpy);
        // find the largest gap between two adjascent numbers
        // this will separate the narrow bars from the wide bars
        int largestGap = Integer.MIN_VALUE;
        int gapIndex = -1;
        for (int i = 0; i < sortedCpy.length - 1; i++) {
            int gap = sortedCpy[i + 1] - sortedCpy[i];
            if (gap > largestGap) {
                largestGap = gap;
                gapIndex = i;
            }
        }
        // something is very wrong if there are no gaps in the input data
        if (gapIndex == -1) badCode(1);
        int narrowMin = sortedCpy[0];
        int narrowMax = sortedCpy[gapIndex];
        int wideMin = sortedCpy[gapIndex + 1];
        int wideMax = sortedCpy[sortedCpy.length - 1];
        return new int[] {narrowMin, narrowMax, wideMin, wideMax};
    }

    /*
      Arg: string array (assumed to be integer strings)
      Returns: int array
    */
    private static int[] convertToIntArray(String[] stringArray) {
        int[] intArray = new int[stringArray.length];
        for (int i = 0; i < intArray.length; i++) {
            intArray[i] = Integer.parseInt(stringArray[i]);
        }
        return intArray;
    }

    private static int getWeight(char character) {
        if (Character.isDigit(character)) {
            return Integer.parseInt(Character.toString(character));
        } else if (character == '-') {
            return 10;
        } else {
            return -1;
        }
    }
    
    private static boolean checkC(char[] characters) {
        int n = characters.length - 2;
        int c = Integer.parseInt(Character.toString(characters[n]));
        int sum = 0;
        for (int i = 0; i < n; i++) {
            sum += ((n - i - 1)%10 + 1)*getWeight(characters[i]);
        }
        sum %= 11;

        return (c == sum);
    }

    private static boolean checkK(char[] characters) {
        int n = characters.length - 1;
        int k = Integer.parseInt(Character.toString(characters[n]));
        int sum = 0;        
        for (int i = 0; i < n; i++) {
            sum += ((n - i - 1)%9 + 1)*getWeight(characters[i]);
        }
        sum %= 11;
        return (k == sum);
    }
    
    /*
      Output 'bad code' error with the given case number,
      and exit w/ error
    */
    private static void badCode(int caseNum) {
        System.out.println("Case " + caseNum + ": bad code");
        System.exit(1);
    }

    private static void badC(int caseNum) {
        System.out.println("Case " + caseNum + ": bad c");
        System.exit(1);
        
    }

    private static void badK(int caseNum) {
        System.out.println("Case " + caseNum + ": bad k");
        System.exit(1);        
    }

    public static void run(String[] args) {
        int[] inputs = argsToInputs(args);
        if (inputs == null) badCode(1);        
        byte[] byteEncodedInputs = convertToByteEncoding(inputs);
        if (byteEncodedInputs == null) badCode(1);
        byteEncodedInputs = trimStartStop(byteEncodedInputs);
        if (byteEncodedInputs == null) badCode(1);
        char[] characters = convertToCharacters(byteEncodedInputs);
        if (characters == null) badCode(1);
        if (!checkC(characters)) badC(1);
        if (!checkK(characters)) badK(1);
        characters = Arrays.copyOfRange(characters, 0, characters.length - 2);
        System.out.println(characters);
    }
    
    public static void main(String[] args) {
        // args.length must be >= 18
        // 1 int for number of regions
        // 10 for the start and stop encodings
        // 5 for the minimum one encoded char,
        // at least 2 separating bars
        if (args.length < 18) badCode(0);
        BarCodes.run(args);
    }
}