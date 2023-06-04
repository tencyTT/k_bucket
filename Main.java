import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
class Peer {
    private final byte[] peerId;
    private byte[] value;
    private final DHT dht;
    public Peer(byte[] peerId, byte[] value) {
        this.peerId = peerId;
        this.value = value;
        this.dht = new DHT();
    }
    public boolean setValue(byte[] key, byte[] value) {
// Check if key is the hash of value
        byte[] hashedValue = hashValue(value);
        if (!Arrays.equals(hashedValue, key)) {
            return false;
        }
// Check if key-value pair is alreadystored
        if (dht.contains(key)) {
            return true;
        }
// Store key-value pairdht.store(key, value);
// Find two closest peers and callsetValue on them recursively
        List<Peer> closestPeers = dht.findClosestPeers(key, 2);
        for (Peer closestPeer : closestPeers) {
            if(!Arrays.equals(closestPeer.getPeerIdBytes(), peerId)) {
                closestPeer.setValue(key, value);
            }
        }
        return true;
    }
    public byte[] getValue() {
        return value;
    }
    public byte[] getValue(byte[] key) {
// Check if value is stored in current peer
        if (dht.contains(key)) {
            return dht.getValue(key);
        }
// Find two closest peers and callgetValue on them
        List<Peer> closestPeers = dht.findClosestPeers(key, 2);
        for (Peer peer : closestPeers) {
            byte[] value = peer.getValue(key);
            if (value != null) {
                return value; // 返回获取到的值，而不是 peer.getPeerIdBytes()
            }
        }
        return null;
    }
    public byte[] getPeerIdBytes() {
        return peerId;
    }
    private byte[] hashValue(byte[] value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            return digest.digest(value);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }
}
class DHT {
    private final List<List<Peer>> buckets;
    public DHT() {
        buckets = new ArrayList<>(160);
        for (int i = 0; i < 160; i++) {
            buckets.add(new ArrayList<>());
        }
    }
    public boolean contains(byte[] key) {
        int bucketIndex = getBucketIndex(key);
        List<Peer> bucket = buckets.get(bucketIndex);
        for (Peer peer : bucket) {
            if (Arrays.equals(peer.getPeerIdBytes(), key)) {
                return true;
            }
        }
        return false;
    }
    public void store(byte[] key, byte[] value) {
        int bucketIndex = getBucketIndex(key);
        List<Peer> bucket = buckets.get(bucketIndex);
        for (Peer peer : bucket) {
            if (Arrays.equals(peer.getPeerIdBytes(), key)) {
                return; // Key already exists, donot store again
            }
        }
        bucket.add(new Peer(key, value));
    }
    public byte[] getValue(byte[] key) {
        int bucketIndex = getBucketIndex(key);
        List<Peer> bucket = buckets.get(bucketIndex);
        for (Peer peer : bucket) {
            if (Arrays.equals(peer.getPeerIdBytes(), key)) {
                return peer.getValue();
            }
        }
        return null;
    }
    public List<Peer> findClosestPeers(byte[] key, int numPeers) {
        int bucketIndex = getBucketIndex(key);
        List<Peer> bucket = buckets.get(bucketIndex);
        bucket.sort((p1, p2) -> Integer.compare(distance(p1.getPeerIdBytes(), key), distance(p2.getPeerIdBytes(), key)));
        return bucket.subList(0, Math.min(numPeers, bucket.size()));
    }
    private int getBucketIndex(byte[] key) {
        return distance(key, key);
    }
    private int distance(byte[] key1, byte[] key2)
    {
        BigInteger xorResult = new BigInteger(1, key1).xor(new BigInteger(1, key2));
        return xorResult.bitCount();
    }
}
public class Main {
    public static void main(String[] args) {
        List<Peer> peers = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            peers.add(new Peer(intToByteArray(i), generateRandomString(10).getBytes()));
        }
        List<String> randomStrings = new ArrayList<>();
        for (int i = 0; i < 200; i++) {
            String randomString = generateRandomString(10);
            randomStrings.add(randomString);
            byte[] value = randomString.getBytes(StandardCharsets.UTF_8);
            byte[] key = hashValue(value);
            Peer peer = peers.get(new Random().nextInt(peers.size()));
            peer.setValue(key, value);
        }
        List<byte[]> randomKeys = new
                ArrayList<>();
        for (int i = 0; i < 100; i++) {
            randomKeys.add(hashValue(randomStrings.get(new Random().nextInt(randomStrings.size())).getBytes(StandardCharsets.UTF_8)));
        }
        for (byte[] key : randomKeys) {
            Peer peer = peers.get(new
                    Random().nextInt(peers.size()));
            byte[] value = peer.getValue(key);
            if (value != null) {
                System.out.println("Key: " + bytesToHexString(key) + " , Value: " + new String(value, StandardCharsets.UTF_8));
            } else {
                System.out.println("Key: " + bytesToHexString(key) + " , Value not found");
            }
        }
    }
    private static byte[] intToByteArray(int value) {
        byte[] result = new byte[4];
        for (int i = 0; i < 4; i++) {
            result[i] = (byte) ((value >> (i * 8)) & 0xFF);
        }
        return result;
    }
    private static String
    generateRandomString(int length) {
        String characters = "abcdefghijklmnopqrstuvwxyz";
        StringBuilder sb = new StringBuilder(length);
        Random random = new Random();
        for (int i = 0; i < length; i++) {
            sb.append(characters.charAt(random.nextInt(characters.length())));
        }
        return sb.toString();
    }
    private static byte[] hashValue(byte[] value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            return digest.digest(value);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }
    private static String bytesToHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X" , b));
        }
        return sb.toString();
    }
}