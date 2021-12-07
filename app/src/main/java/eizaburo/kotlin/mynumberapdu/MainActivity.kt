package eizaburo.kotlin.mynumberapdu

import android.app.AlertDialog
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.*
import android.nfc.tech.IsoDep

import android.graphics.BitmapFactory

import android.graphics.Bitmap
import android.widget.ImageView
import org.bouncycastle.asn1.ASN1Sequence
import org.bouncycastle.asn1.DLApplicationSpecific
import org.bouncycastle.util.encoders.Hex
import java.lang.Error
import java.nio.ByteBuffer
import org.bouncycastle.asn1.ASN1StreamParser
import com.gemalto.jp2.JP2Decoder;
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {
    val scope = CoroutineScope(Dispatchers.Default)
    private var mNfcAdapter: NfcAdapter? = null
    private var pendingIntent: PendingIntent? = null
    private var intentFilters: Array<IntentFilter>? = null
    private var techLists: Array<Array<String>>? = null
    private var txt_name:TextView?=null
    private var txt_address:TextView?=null
    private var txt_birthday:TextView?=null
    private var txt_sex:TextView?=null
    private var txt_mynumber:TextView?=null
    private var editPin:EditText?=null
    private var imgPhoto:ImageView?=null
    private var imgAddr:ImageView?=null
    private var imgName:ImageView?=null
    private var tries=0
    private var myNumber=""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Log.d("MainActivity","Created")

        val intent = Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_MUTABLE)

        // 受け取るIntentを指定
        intentFilters = arrayOf(IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED))

        // 反応するタグの種類を指定
        techLists = arrayOf(
            arrayOf(android.nfc.tech.NfcB::class.java.name),
            arrayOf(android.nfc.tech.NfcA::class.java.name),
            arrayOf(android.nfc.tech.NfcF::class.java.name),
            arrayOf(android.nfc.tech.NfcV::class.java.name),
            arrayOf(android.nfc.tech.NfcBarcode::class.java.name),
            arrayOf(android.nfc.tech.NdefFormatable::class.java.name),
            arrayOf(android.nfc.tech.Ndef::class.java.name),
            arrayOf(android.nfc.tech.IsoDep::class.java.name))

        mNfcAdapter = NfcAdapter.getDefaultAdapter(applicationContext)

        txt_name = findViewById(R.id.txt_name) as TextView
        txt_address = findViewById(R.id.txt_address) as TextView
        txt_birthday = findViewById(R.id.txt_birthday) as TextView
        txt_sex = findViewById(R.id.txt_sex) as TextView
        txt_mynumber=findViewById(R.id.txt_mynumber) as TextView
        editPin = findViewById(R.id.edit_pin) as EditText
        imgPhoto=findViewById(R.id.img_photo) as ImageView
        imgName=findViewById(R.id.img_name) as ImageView
        imgAddr=findViewById(R.id.img_addr) as ImageView

    }

    override fun onResume() {
        super.onResume()
        // NFCタグの検出を有効化
        if (mNfcAdapter == null) {
            return;
        }
        mNfcAdapter?.enableForegroundDispatch(this, pendingIntent, intentFilters, techLists)

    }

    fun initVisualReader(isoDep: IsoDep){
        println("# SELECT VISUAL FILE: 券面入力補助AP (DF)")
        // Bytes sequence command
        val bid= Hex.decode("D3921000310001010402")
        // create Byte Header
        var header = ByteBuffer.allocate(4)
        // 00 A4 04 0C
        header.put(0.toByte()).put(164.toByte()).put(4.toByte()).put(12.toByte())
        // Create Byte Data
        var data=ByteBuffer.allocate(1 + bid!!.size)
        // 0A = 10
        data.put(10.toByte())
        data.put(bid)

        // Combinate both buffer
        var buf = ByteBuffer.allocate(header.limit() + data.limit())
        buf.put(header.rewind() as ByteBuffer)
        buf.put(data.rewind() as ByteBuffer)

        val res: ByteArray = isoDep.transceive(buf.array())

        //val dataResponse:ByteArray= Arrays.copyOfRange(res, 0, res.size - 2)
        // transform byte to unsigned value
        var sw1=res[res.size - 2].toInt() and 255
        var sw2=res[res.size -1].toInt() and 255
        var formattedRes=String.format("%02X %02X", sw1, sw2)
        println(formattedRes)

        println("# SELECT VISUAL FILE: 券面入力補助用PIN (EF)")
        // Select File cmd
        var headerSelectFileEF = ByteBuffer.allocate(4)
        // 00 A4 02 0C
        headerSelectFileEF.put(0.toByte()).put(164.toByte()).put(2.toByte()).put(12.toByte())
        // Create Byte Data
        var dataSelectFileEF = ByteBuffer.allocate(3)
        // 02 00 13
        dataSelectFileEF.put(2.toByte())
        dataSelectFileEF.put(0.toByte())
        dataSelectFileEF.put(19.toByte())
        var bufSelectFileEF=ByteBuffer.allocate(headerSelectFileEF.limit() + dataSelectFileEF.limit())
        bufSelectFileEF.put(headerSelectFileEF.rewind() as ByteBuffer)
        bufSelectFileEF.put(dataSelectFileEF.rewind() as ByteBuffer)

        val resSelectFileEF: ByteArray = isoDep.transceive(bufSelectFileEF.array())
        sw1=resSelectFileEF[resSelectFileEF.size - 2].toInt() and 255
        sw2=resSelectFileEF[resSelectFileEF.size -1].toInt() and 255
        formattedRes=String.format("%02X %02X", sw1, sw2)
        println(formattedRes)
    }

    fun readVisualFiles(isoDep:IsoDep){
        println("# SELECT FILE: マイナンバー (EF)")
        var header = ByteBuffer.allocate(4)
        // 00 A4 02 0C / 02 00 01
        header.put(0.toByte()).put(164.toByte()).put(2.toByte()).put(12.toByte())
        // Create Byte Data
        var data = ByteBuffer.allocate(3)
        data.put(2.toByte())
        data.put(0.toByte())
        data.put(1.toByte())
        var buf=ByteBuffer.allocate(header.limit() + data.limit())
        buf.put(header.rewind() as ByteBuffer)
        buf.put(data.rewind() as ByteBuffer)

        val res: ByteArray = isoDep.transceive(buf.array())
        val sw1=res[res.size - 2].toInt() and 255
        val sw2=res[res.size -1].toInt() and 255
        val formattedRes=String.format("%02X %02X", sw1, sw2)
        println(formattedRes)
    }

    fun initReader(isoDep:IsoDep){
        println("# SELECT FILE: 券面入力補助AP (DF)")
        // Bytes sequence command
        val bid= Hex.decode("D3921000310001010408")
        // create Byte Header
        var header = ByteBuffer.allocate(4)
        // 00 A4 04 0C
        header.put(0.toByte()).put(164.toByte()).put(4.toByte()).put(12.toByte())
        // Create Byte Data
        var data=ByteBuffer.allocate(1 + bid!!.size)
        // 0A = 10
        data.put(10.toByte())
        data.put(bid)

        // Combinate both buffer
        var buf = ByteBuffer.allocate(header.limit() + data.limit())
        buf.put(header.rewind() as ByteBuffer)
        buf.put(data.rewind() as ByteBuffer)

        val res: ByteArray = isoDep.transceive(buf.array())

        //val dataResponse:ByteArray= Arrays.copyOfRange(res, 0, res.size - 2)
        // transform byte to unsigned value
        var sw1=res[res.size - 2].toInt() and 255
        var sw2=res[res.size -1].toInt() and 255
        var formattedRes=String.format("%02X %02X", sw1, sw2)
        println(formattedRes)
        // if r1r2 = 90 00 => Command success

        println("# SELECT FILE: 券面入力補助用PIN (EF)")
        // Select File cmd
        var headerSelectFileEF = ByteBuffer.allocate(4)
        // 00 A4 02 0C
        headerSelectFileEF.put(0.toByte()).put(164.toByte()).put(2.toByte()).put(12.toByte())
        // Create Byte Data
        var dataSelectFileEF = ByteBuffer.allocate(3)
        // 02 00 11
        dataSelectFileEF.put(2.toByte())
        dataSelectFileEF.put(0.toByte())
        dataSelectFileEF.put(17.toByte())
        var bufSelectFileEF=ByteBuffer.allocate(headerSelectFileEF.limit() + dataSelectFileEF.limit())
        bufSelectFileEF.put(headerSelectFileEF.rewind() as ByteBuffer)
        bufSelectFileEF.put(dataSelectFileEF.rewind() as ByteBuffer)

        val resSelectFileEF: ByteArray = isoDep.transceive(bufSelectFileEF.array())
        sw1=resSelectFileEF[resSelectFileEF.size - 2].toInt() and 255
        sw2=resSelectFileEF[resSelectFileEF.size -1].toInt() and 255
        formattedRes=String.format("%02X %02X", sw1, sw2)
        println(formattedRes)
    }

    fun verifyVisualPin(isoDep:IsoDep):Boolean{
        // if no Current EF returns 69 86
        if(myNumber!=="") {
            var bp1 = 0x30.plus(myNumber[0].digitToInt())
            val bp2 = 0x30.plus(myNumber[1].digitToInt())
            val bp3 = 0x30.plus(myNumber[2].digitToInt())
            val bp4 = 0x30.plus(myNumber[3].digitToInt())
            var bp5 = 0x30.plus(myNumber[4].digitToInt())
            val bp6 = 0x30.plus(myNumber[5].digitToInt())
            val bp7 = 0x30.plus(myNumber[6].digitToInt())
            val bp8 = 0x30.plus(myNumber[7].digitToInt())
            val bp9 = 0x30.plus(myNumber[8].digitToInt())
            val bp10 = 0x30.plus(myNumber[9].digitToInt())
            val bp11 = 0x30.plus(myNumber[10].digitToInt())
            val bp12 = 0x30.plus(myNumber[11].digitToInt())
            println("# VERIFY VISUAL PIN: 券面入力補助用PIN")
            // VERIFY COMMAND
            var header = ByteBuffer.allocate(4)
            // 00 20 00 80 + pin
            header.put(0.toByte()).put(32.toByte()).put(0.toByte()).put(128.toByte())
            // Create Byte Data
            var data = ByteBuffer.allocate(13)
            // put password
            data.put(12.toByte()) // Length of data
            data.put(bp1.toByte())
            data.put(bp2.toByte())
            data.put(bp3.toByte())
            data.put(bp4.toByte())
            data.put(bp5.toByte())
            data.put(bp6.toByte())
            data.put(bp7.toByte())
            data.put(bp8.toByte())
            data.put(bp9.toByte())
            data.put(bp10.toByte())
            data.put(bp11.toByte())
            data.put(bp12.toByte())

            var buf = ByteBuffer.allocate(header.limit() + data.limit())
            buf.put(header.rewind() as ByteBuffer)
            buf.put(data.rewind() as ByteBuffer)

            val res: ByteArray = isoDep.transceive(buf.array())
            val sw1 = res[res.size - 2].toInt() and 255
            val sw2 = res[res.size - 1].toInt() and 255

            val formattedRes = String.format("%02X %02X", sw1, sw2)
            println(formattedRes)
            // if SW1 = 99 -> PIN incorrect
            if (sw1 == 99) {
                val formattedsw2 = String.format("%02X", sw2)
                val numberofTriesLeft = formattedsw2[1].digitToInt()
                tries = numberofTriesLeft
                return false
            } else if (sw1 == 144 && sw2 == 0) {
                return true
            } else
                return false
        }
        else{
            return false
        }
    }

    fun verifyPin(isoDep:IsoDep): Boolean {
        val pin: String = editPin?.getText().toString()

        var bp1=0x30.plus(pin[0].digitToInt())
        val bp2=0x30.plus(pin[1].digitToInt())
        val bp3=0x30.plus(pin[2].digitToInt())
        val bp4=0x30.plus(pin[3].digitToInt())
        println("# VERIFY: 券面入力補助用PIN")
        // VERIFY COMMAND
        var header = ByteBuffer.allocate(4)
        // 00 20 00 80 + pin
        header.put(0.toByte()).put(32.toByte()).put(0.toByte()).put(128.toByte())
        // Create Byte Data
        var data = ByteBuffer.allocate(5)
        // put password
        data.put(4.toByte()) // Length of data
        data.put(bp1.toByte())
        data.put(bp2.toByte())
        data.put(bp3.toByte())
        data.put(bp4.toByte())

        var buf=ByteBuffer.allocate(header.limit() + data.limit())
        buf.put(header.rewind() as ByteBuffer)
        buf.put(data.rewind() as ByteBuffer)

        val res: ByteArray = isoDep.transceive(buf.array())
        val sw1=res[res.size - 2].toInt() and 255
        val sw2=res[res.size -1].toInt() and 255

        val formattedRes=String.format("%02X %02X", sw1, sw2)
        println(formattedRes)
        // if SW1 = 99 -> PIN incorrect
        if(sw1==99) {
            val formattedsw2=String.format("%02X",sw2)
            val numberofTriesLeft=formattedsw2[1].digitToInt()
            tries=numberofTriesLeft
            return false
        }
        else if (sw1==144 && sw2==0){
            return true
        }
        else
            return false

    }

    fun selectMyNumberFile(isoDep: IsoDep){
        println("# SELECT FILE: マイナンバー (EF)")
        var header = ByteBuffer.allocate(4)
        // 00 A4 02 0C / 02 00 01
        header.put(0.toByte()).put(164.toByte()).put(2.toByte()).put(12.toByte())
        // Create Byte Data
        var data = ByteBuffer.allocate(3)
        data.put(2.toByte())
        data.put(0.toByte())
        data.put(1.toByte())
        var buf=ByteBuffer.allocate(header.limit() + data.limit())
        buf.put(header.rewind() as ByteBuffer)
        buf.put(data.rewind() as ByteBuffer)

        val res: ByteArray = isoDep.transceive(buf.array())
        val sw1=res[res.size - 2].toInt() and 255
        val sw2=res[res.size -1].toInt() and 255
        val formattedRes=String.format("%02X %02X", sw1, sw2)
        println(formattedRes)
    }

    fun getMyNumber(isoDep: IsoDep){
        println("# READ BINARY: マイナンバー読み取り（4～15バイト目が個人番号）\n")
        // MyNumberCardRead
        val header=ByteBuffer.allocate(5)
        // 00 B0 00 00 00
        header.put(0.toByte()).put(176.toByte()).put(0.toByte()).put(0.toByte()).put(0.toByte())
        var buf=ByteBuffer.allocate(header.limit() )
        buf.put(header.rewind() as ByteBuffer)

        val res: ByteArray = isoDep.transceive(buf.array())
        val sw1=res[res.size - 2].toInt() and 255
        val sw2=res[res.size -1].toInt() and 255
        val MyNumber:ByteArray= Arrays.copyOfRange(res, 3, 15)
        println(String(MyNumber))
        txt_mynumber?.text=String(MyNumber)
        myNumber=String(MyNumber)
        val formattedRes=String.format("%02X %02X", sw1, sw2)
        println(formattedRes)
    }

    fun selectFileData(isoDep:IsoDep){
        println("# SELECT FILE: 基本4情報 (EF)")
        var header = ByteBuffer.allocate(4)
        // 00 A4 02 0C / 02 00 02
        header.put(0.toByte()).put(164.toByte()).put(2.toByte()).put(12.toByte())
        // Create Byte Data
        var data = ByteBuffer.allocate(3)
        data.put(2.toByte())
        data.put(0.toByte())
        data.put(2.toByte())
        var buf=ByteBuffer.allocate(header.limit() + data.limit())
        buf.put(header.rewind() as ByteBuffer)
        buf.put(data.rewind() as ByteBuffer)

        val res: ByteArray = isoDep.transceive(buf.array())
        val sw1=res[res.size - 2].toInt() and 255
        val sw2=res[res.size -1].toInt() and 255
        val formattedRes=String.format("%02X %02X", sw1, sw2)
        println(formattedRes)
    }

    fun readBinaryData(isoDep:IsoDep){
        println("# READ BINARY: 基本4情報の読み取り")
        val header=ByteBuffer.allocate(5)
        // 00 B0 00 02 01
        header.put(0.toByte()).put(176.toByte()).put(0.toByte()).put(0.toByte()).put(0.toByte())
        var buf=ByteBuffer.allocate(header.limit() )
        buf.put(header.rewind() as ByteBuffer)

        val res: ByteArray = isoDep.transceive(header.array())
        val r=res[0].toInt() and 255
        val sw1=res[res.size - 2].toInt() and 255
        val sw2=res[res.size -1].toInt() and 255
        val rr=String.format("%02X", r)
        println(rr)
        val formattedRes=String.format("%02X %02X", sw1, sw2)
        println(formattedRes)
    }

    fun getBasicData(isoDep: IsoDep){
        println("# READ BINARY: 基本4情報の読み取り")
        // MyNumberCardRead
        val header=ByteBuffer.allocate(5)
        // 00 B0 00 00 85
        header.put(0.toByte()).put(176.toByte()).put(0.toByte()).put(0.toByte()).put(255.toByte())
        var buf=ByteBuffer.allocate(header.limit() )
        buf.put(header.rewind() as ByteBuffer)

        val res: ByteArray = isoDep.transceive(buf.array())
        val sw1=res[res.size - 2].toInt() and 255
        val sw2=res[res.size -1].toInt() and 255
        val Data:ByteArray= Arrays.copyOfRange(res, 19, res.size-2)

        val dataString=Data.toString(Charsets.UTF_8)
        val array=dataString.split("�").toTypedArray()
        val name=array[0]
        val address=array[1]
        val birthday=array[2]
        val sex=array[3]
        println(name)
        println(address.replace("#`",""))
        println(birthday)
        println(sex.replace("%",""))
        // 1 male, 2 female else other
        txt_name?.text=name.trim()
        txt_address?.text=address.replace("\u0001","").replace("#`","").trim()
        txt_birthday?.text=birthday.replace("\u0001","").replace("$","").trim()
        var sexString=""
        val s=sex.replace("%","").replace("\u0001","").trim()
        when(s){
            "1"->{
                sexString="男性"
            }
            "2"->{
                sexString="女性"
            }
            else->{
                sexString="その他"
            }
        }
        txt_sex?.text=sexString

        val formattedRes=String.format("%02X %02X", sw1, sw2)
        println(formattedRes)
    }

    fun selectVisualFiles(isoDep:IsoDep){
        println("# SELECT Visual FILE (EF)")
        var header = ByteBuffer.allocate(4)
        // 00 A4 02 0C / 02 00 02
        header.put(0.toByte()).put(164.toByte()).put(2.toByte()).put(12.toByte())
        // Create Byte Data
        var data = ByteBuffer.allocate(3)
        data.put(2.toByte())
        data.put(0.toByte())
        data.put(2.toByte())
        var buf=ByteBuffer.allocate(header.limit() + data.limit())
        buf.put(header.rewind() as ByteBuffer)
        buf.put(data.rewind() as ByteBuffer)

        val res: ByteArray = isoDep.transceive(buf.array())
        val sw1=res[res.size - 2].toInt() and 255
        val sw2=res[res.size -1].toInt() and 255
        val formattedRes=String.format("%02X %02X", sw1, sw2)
        println(formattedRes)
    }
    fun readVisualFileBinary(isoDep: IsoDep): ByteArray {
        // Loop until every files has been read, sequence done when response is filled with -1
        println("# READ BINARY VISUAL FILE")
        val bs = ByteArrayOutputStream()

        var fileRead=false
        var i =0
        do{
            var header=ByteBuffer.allocate(5)
            //header.put(0.toByte()).put(176.toByte()).put(0.toByte()).put(0.toByte()).put(0.toByte())
            header.put(0.toByte()).put(176.toByte()).put(i.toByte()).put(0.toByte()).put(0.toByte())
            var res: ByteArray = isoDep.transceive(header.array())
            println(res.size)
            var sw1=res[res.size - 2].toInt() and 255
            var sw2=res[res.size -1].toInt() and 255
            var formattedRes=String.format("%02X %02X", sw1, sw2)
            println(formattedRes)
            if(res.size>2){
                var byteCompare= res[res.size-3]== (-1).toByte()
                var data:ByteArray?
                data = Arrays.copyOfRange(res, 0, res.size - 2)
                bs.write(data)
                fileRead=byteCompare
            }
            i++

        }while(!fileRead)

        return Arrays.copyOfRange(bs.toByteArray(),0,bs.size())
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        runOnUiThread {
            txt_mynumber?.text="読み取り開始、カードを離さないでください"
            txt_name?.text=""
            txt_address?.text=""
            txt_birthday?.text=""
            txt_sex?.text=""
            imgPhoto?.setImageBitmap(null)
            imgAddr?.setImageBitmap(null)
            imgName?.setImageBitmap(null)

        }
        // タグのIDを取得
        val pin: String = editPin?.getText().toString()

        if(pin.length!==4){
            txt_mynumber?.text="PINコードを入力して下さい"
        }
        else {

            try {
                val tag: Tag? = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
                println("TAG Discovered")

                val isoDep = IsoDep.get(tag)
                isoDep.connect()
                println("Connected isodep")
                scope.launch {
                    readTask(isoDep)
                }
                // Protocol https://tex2e.github.io/blog/protocol/jpki-mynumbercard-with-apdu
            } catch (e: Error) {
                println(e.message)
                txt_mynumber?.text = "読み取りエラーが発生しました。"
            }catch(e:IOException){
                txt_mynumber?.text = "読み取りエラー、タッグが失われました。"
            }
        }
    }

    private suspend fun readTask(isoDep:IsoDep){
        try {
            withContext(Dispatchers.Main) {
                initReader(isoDep)
                var res = verifyPin(isoDep)

                if (res) {

                    // Text Files for MyNumber + 4 basic data
                    selectMyNumberFile(isoDep)
                    getMyNumber(isoDep)
                    selectFileData(isoDep)
                    readBinaryData(isoDep)
                    getBasicData(isoDep)

                    // Visual Data functions
                    initVisualReader(isoDep)

                    verifyVisualPin(isoDep)
                    selectVisualFiles(isoDep)
                    val fileData = readVisualFileBinary(isoDep)
                    decode(fileData)
                } else {
                    txt_mynumber?.text = "PINコードが間違っています。残り" + tries + "回"
                }
            }

            withContext(Dispatchers.Main) {

            }
        }catch(e:IOException){
            txt_mynumber?.text = "読み取りエラー、タッグが失われました。"
        }
    }

    override fun onPause() {
        super.onPause()
        if (mNfcAdapter == null) {
            return;
        }
        mNfcAdapter?.disableForegroundDispatch(this)
    }

    @Throws(IOException::class)
    private fun decode(encoded:ByteArray) {
        val parser = ASN1StreamParser(encoded)
        val objAll = parser.readObject() as DLApplicationSpecific
        val seq = objAll.getObject(16) as ASN1Sequence
        val e = seq.objects
        while (e.hasMoreElements()) {
            val obj = e.nextElement() as DLApplicationSpecific
            when (obj.applicationTag) {
                37 ->{ // name
                    val nameImg=BitmapFactory.decodeByteArray(obj.contents,0,obj.contents.size)
                    imgName?.setImageBitmap(nameImg)
                }
                38 -> { // addr
                    val addrImg=BitmapFactory.decodeByteArray(obj.contents,0,obj.contents.size)
                    imgAddr?.setImageBitmap(addrImg)
                }
                39 ->{ // photo
                    // Need to decode photo data
                    //val photoImg=BitmapFactory.decodeByteArray(obj.contents,0,obj.contents.size)
                    val photoImg= JP2Decoder.isJPEG2000(obj.contents)
                    val bmp: Bitmap = JP2Decoder(obj.contents).decode()
                    imgPhoto?.setImageBitmap(bmp)
                }
            }
        }
    }
}