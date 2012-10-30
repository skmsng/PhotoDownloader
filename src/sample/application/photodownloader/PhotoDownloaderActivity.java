package sample.application.photodownloader;

import android.app.Activity;
import android.os.Bundle;
import android.app.DownloadManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.os.AsyncTask;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.TextView;
import java.net.Authenticator;
import java.net.PasswordAuthentication;

public class PhotoDownloaderActivity extends Activity {

	Context mContext = this;
	Object[] objects;	//JSON配列から取り出したJSONオブジェクトを格納する配列
	static  String strUrl=
			"https://api.datamarket.azure.com/Bing/Search/Image?$format=json&$top=10";
	//<アカウントキー>を自分のアカウントキーに置き換える必要があります
	char[] accountKey="vl1VdUpZI4lTT/TtovfHzxjRAa6tazcy17/yjg84N7I=".toCharArray();
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        TextView tv = (TextView)findViewById(R.id.textView1);
		tv.setText(Html.fromHtml(getResources().getString(R.string.label_branding)));
		tv.setMovementMethod(LinkMovementMethod.getInstance());
		
		//ボタンにリスナー登録
		View button1 = findViewById(R.id.button1);
		button1.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				doSearch(); 
			}
		});
		//Enterキーにリスナー登録
		View editText1 = findViewById(R.id.editText1);
		editText1.setOnKeyListener(new OnKeyListener(){
			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if(event.getAction()==KeyEvent.ACTION_UP &&
						keyCode==KeyEvent.KEYCODE_ENTER){
					doSearch();
					return true;
				}
				return false;
			}
		});
		
		Authenticator.setDefault(
				new Authenticator() {
					protected PasswordAuthentication getPasswordAuthentication() {
						return new PasswordAuthentication("", accountKey);
					}
				}
		);
    }

    //strUrl+入力文字列でjsonTaskを呼び出す
    void doSearch(){
		URL url;
		String q = null;
		EditText et = (EditText)this.findViewById(R.id.editText1);
		try {
			q = URLEncoder.encode(et.getText().toString(), "UTF-8");//入力された文字をエンコード
			url = new URL(strUrl+"&Query='"+q+"'");
			new jsonTask().execute(url);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
	}
    
    //インナークラス
    //AsyncTask（スレッドをAndroidに特化したようなもの）
    private class jsonTask extends AsyncTask<URL, Integer, String> {

    	//params配列の要素は１つだけ
    	//可変長引数（URL...とURL[]の意味は同じ）...呼び出し時に引数が配列じゃなくていい
		@Override
		protected String doInBackground(URL... params) {
			HttpURLConnection connection = null;
			try {
				//サーバーに接続
				connection = (HttpURLConnection) params[0].openConnection();
				connection.setDoInput(true);
				connection.connect();
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(connection.getInputStream(),"UTF-8"));
				//BingAPIの検索結果は1行で返されるので、1行だけ読み取る
				String jsonText = reader.readLine();
				reader.close();
				return(jsonText);
			} catch (Exception e) {
				e.printStackTrace();
			}
			finally{
				if(connection != null){
					connection.disconnect();
				}
			}
			return null;
		}

		//resultには、上記doInBackground()の結果（JSON形式データ）が格納されている
		@Override
		protected void onPostExecute(String result) {
			// TODO 自動生成されたメソッド・スタブ
			super.onPostExecute(result);
			if(result != null){
				try {
					//JSONオブジェクトを作成し、検索結果を取得
					JSONObject jo = new JSONObject(result).getJSONObject("d");
					//検索結果の配列に分解して格納
					JSONArray jsonArray = null;
					jsonArray = jo.getJSONArray("results");
					//JSON配列をObject型配列に格納し直すための配列初期化
					objects = new Object[jsonArray.length()];
					//JSON配列の画像URLをURL型配列に格納し直すための配列初期化
					URL[] thumbUrls = new URL[jsonArray.length()];
					
					//JSON配列から取り出したJSONオブジェクトを、Object配列とURL配列に格納
					for(int i=0; i<jsonArray.length(); i++){
						objects[i] = jsonArray.getJSONObject(i);
						thumbUrls[i] = new URL(((JSONObject)objects[i]).
								getJSONObject("Thumbnail").getString("MediaUrl"));
					}
					new getThumbTask().execute(thumbUrls);
				} catch (JSONException e1) {
					e1.printStackTrace();
				} catch (MalformedURLException e) {
					e.printStackTrace();
				}
			}
		}
	}
    
    //インナークラス
    private class getThumbTask extends AsyncTask<URL, Integer, Bitmap[]> {

		@Override
		protected Bitmap[] doInBackground(URL... params) {
			HttpURLConnection connection = null;
			Bitmap[] bm = new Bitmap[params.length];
			try {
				for(int i=0; i<params.length; i++){
					//サーバーに接続
					connection = (HttpURLConnection) params[i].openConnection();
					connection.setDoInput(true);
					connection.connect();
					//読み込んだ画像をBitmap配列に格納
					InputStream si = connection.getInputStream();
					bm[i] = BitmapFactory.decodeStream(si);
					si.close();
				}
				return bm;
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				if(connection!=null)connection.disconnect();
			}
			return null;
		}

		@Override
		protected void onPostExecute(Bitmap[] result) {
			super.onPostExecute(result);
			
			final ThumbnailAdapter adapter = new ThumbnailAdapter(mContext, objects, result);
			GridView lv = (GridView)findViewById(android.R.id.list);
			lv.setAdapter(adapter);
			//リスナー登録（画像をダウンロードする）
			lv.setOnItemClickListener(new OnItemClickListener() {
				public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
					try {
						Uri downloadUri = Uri.parse(
								((JSONObject)adapter.getItem(position)).getString("MediaUrl"));
						//DownloadManagerを使ってダウンロード
						DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
						DownloadManager.Request dr = new DownloadManager.Request(downloadUri);
						String fn = URLDecoder.decode(downloadUri.getLastPathSegment(),"UTF-8");
						dr.setDestinationInExternalPublicDir(
								Environment.DIRECTORY_DOWNLOADS, fn);
						dm.enqueue(dr);
					} catch (JSONException e) {
						e.printStackTrace();
					} catch (UnsupportedEncodingException e) {
						e.printStackTrace();
					}
				}
			});
		}
	}

}
