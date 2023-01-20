package kr.uracle.ums.fpc.utils;

import kr.msp.util.AES128Cipher;


public class AESCipherManager {
    public static AESCipherManager getInstance() {
        return AESCipherManager.Singleton.instance;
    }
    private static class Singleton{
        private static final AESCipherManager instance = new AESCipherManager();
    }

    public String AES128_Encode(String plainTxt){
        String encStr = "";
        try{
            encStr = AES128Cipher.AES_Encode(plainTxt);
        }catch (Exception e){
            e.printStackTrace();
            return plainTxt;
        }
        return encStr;
    }

    public String AES128_Decode(String encTxt){
        String plainStr = "";
        try{
            plainStr = AES128Cipher.AES_Decode(encTxt);
        }catch (Exception e){
            e.printStackTrace();
            return encTxt;
        }
        return plainStr;
    }
    
    public static void main(String[] args){
        if(args.length<=0){
            System.out.println("암호화 할 문자열 입력하세요");
        }
        String plainText = args[0];
        String encryptText = AESCipherManager.getInstance().AES128_Encode(plainText);
        System.out.println("["+plainText+"] = ["+encryptText+"]");
    }
    
}
