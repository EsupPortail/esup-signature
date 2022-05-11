package org.esupportail.esupsignature.service.utils.barcode;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.datamatrix.DataMatrixWriter;
import org.apache.commons.codec.binary.Base32;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.DLSequence;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.util.encoders.Hex;
import org.esupportail.esupsignature.exception.EsupSignatureException;
import org.esupportail.esupsignature.service.utils.file.FileService;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.util.Arrays;
import java.util.Date;
import java.util.EnumMap;
import java.util.Map;

@Service
public class DdDocService {

    @Resource
    private FileService fileService;

    public BitMatrix getQrCodeSvg(String value) throws Exception {

//        value = "DC03FR000001123F1636000126FR245700010MLLE/SAMPLE/ANGELA<GS>20<GS>21BAT 2 ETG 3<GS>227 PLACE DES SPECIMENS<GS>23<GS>25METZ<GS><US>3HJIYP3OAJ4LIZNQXCTZMNQPTT5C2XICTEF4UGJ3NDE2CWM7HJOEEK4ACIY4CZOO5ZOFG35APDZMZQFEAEBWRZTW4CBPG35JE2FJ4EY";
        value = generate2dDoc(new Date(), new Date());
        value = preEncode(value);

        Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.MARGIN, 0);

        return new DataMatrixWriter().encode(value, BarcodeFormat.DATA_MATRIX, 400, 400, hints);

    }

    public String generate2dDoc(Date createDate, Date signDate) throws Exception {
        String string2dDoc = "DC";
        string2dDoc += "03";
        string2dDoc += "FR00";
        string2dDoc += "0001";
        DateTimeFormatter formatter = DateTimeFormat.forPattern("dd/MM/yyyy");
        DateTime dt = formatter.parseDateTime("01/01/2000");
        int createDays = Days.daysBetween(LocalDate.fromDateFields(dt.toDate()), LocalDate.fromDateFields(createDate)).getDays();
        int signDays = Days.daysBetween(LocalDate.fromDateFields(dt.toDate()), LocalDate.fromDateFields(signDate)).getDays();
        string2dDoc += Integer.toHexString(createDays).toUpperCase();
        string2dDoc += Integer.toHexString(signDays).toUpperCase();
        string2dDoc += "B0"; //diplome
        string2dDoc += "01"; //diplome
        string2dDoc += "B1DAVID<GS>";
        string2dDoc += "B2LEMAIGNENT<GS>";
        string2dDoc += "B6M";
        string2dDoc += "B729061978";
        string2dDoc += "B9FR";
        string2dDoc += "BD7";
        string2dDoc += "BGMA";
        string2dDoc += "BHJAVA SPRING<GS>";
        string2dDoc += "BIINFORMATIQUE<GS>";
        string2dDoc += "BJINFORMATIQUE DU LOGICIEL<GS>";
        string2dDoc += "B8ROUEN<GS>";
        string2dDoc += "BB55555555<GS>";
        string2dDoc += "BC1234567<GS>";
        string2dDoc += "BF7DF";

//        string2dDoc = "DC03FR000001123F1636000126FR245700010MLLE/SAMPLE/ANGELA<GS>20<GS>21BAT 2 ETG 3<GS>227 PLACE DES SPECIMENS<GS>23<GS>25METZ<GS>";
        String signed2dDoc = sign(preEncode(string2dDoc));
        string2dDoc += "<US>";
        string2dDoc += signed2dDoc;

        return string2dDoc;
    }

    private String preEncode(String codewords) {
        String result = codewords.replaceAll("<GS>", Character.toString((char) 29));
        result = result.replaceAll("<US>", Character.toString((char) 31));
        result = result.replaceAll("<RS>", Character.toString((char) 30));
        result = result.replaceAll("<Espace>", Character.toString((char) 32));
        return result;
    }

    private String sign(String value) throws Exception {

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
        String sha256hex = new String(Hex.encode(hash));
        System.out.println (sha256hex);
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());

        ECPrivateKey pk = (ECPrivateKey) getPrivateKey();
        ECPublicKey pub = (ECPublicKey) getPublicKey();

        System.out.println (pk.getAlgorithm());
        Signature dsa = Signature.getInstance("NONEwithECDSA");
        dsa.initSign(pk);
        dsa.update(hash);
        byte[] signature = dsa.sign();

        DLSequence asn1 = (DLSequence) DERSequence.fromByteArray(signature);
        byte[] a = ((ASN1Integer) asn1.toArray()[0]).getValue().toByteArray();
        byte[] b = ((ASN1Integer) asn1.toArray()[1]).getValue().toByteArray();
        a = Arrays.copyOfRange(a, a.length - 32, a.length);
        b = Arrays.copyOfRange(b, b.length - 32, b.length);
        byte[] c = new byte[a.length + b.length];
        System.arraycopy(a, 0, c, 0, a.length);
        System.arraycopy(b, 0, c, a.length, b.length);

        dsa.initVerify(pub);
        dsa.update(hash);
        if(!dsa.verify(signature)) {
            throw new EsupSignatureException("2ddoc signature error, please check private key and certificate (/resources/2ddoc.key and /resources/2ddoc.cert)");
        }

        Base32 base32 = new Base32();
        return base32.encodeAsString(c).replace("=", "");
    }

    private PrivateKey getPrivateKey() throws Exception {
        BufferedReader br = new BufferedReader(new InputStreamReader(new ClassPathResource("/2ddoc.key").getInputStream()));
        PEMParser pp = new PEMParser(br);
        PEMKeyPair pemKeyPair = (PEMKeyPair) pp.readObject();
        JcaPEMKeyConverter jcaPEMKeyConverter = new JcaPEMKeyConverter();
        jcaPEMKeyConverter.setProvider(new BouncyCastleProvider());
        return jcaPEMKeyConverter.getPrivateKey(pemKeyPair.getPrivateKeyInfo());

    }

    private PublicKey getPublicKey() throws Exception {
        CertificateFactory fact = CertificateFactory.getInstance("X.509");
        X509Certificate cer = (X509Certificate) fact.generateCertificate(new ClassPathResource("/2ddoc.cert").getInputStream());
        return cer.getPublicKey();
    }

}
