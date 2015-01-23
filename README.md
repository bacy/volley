volley
======
###1.支持http 大文件上传以及下载，支持断点下载，下载中允许暂停，下次从暂停地方开始下载
####普通http请求
本来有八种谓词，考虑其他几种不常见，项目中用不上，暂时不提供。
HttpTools提供get，post，upload，download，delete多种请求的封装，一行代码搞定各种异步请求
<p><code>
    get(RequestInfo requestInfo, final IOnHttpResultListener httpResult);  
    post(RequestInfo requestInfo, final IOnHttpResultListener httpResult);  
    delete(RequestInfo requestInfo, final IOnHttpResultListener httpResult);  
    put(RequestInfo requestInfo, final IOnHttpResultListener httpResult);
</code></p>

####文件下载
<p><code>
DownloadRequest download(String url, String target, final boolean isResume, final IOnHttpResultListener httpResult)
</code></p>
设置参数isResume为true，即可实现断点续传，DownloadRequest提供stopDownload方法，可以随时停止当前的下载任务，再次下载将会从上次下载的地方开始下载。quitDownloadQueue允许强制关闭下载线程池，退出下载。可以在所有下载任务完成后关闭，节约资源。

####文件上传
<p><code>
MultiPartRequest<String> upload(final String url, final Map<String, Object> params, final IOnHttpResultListener httpResult)
</code></p>
Params是表单参数，可以传入string和File类型的参数。当多个file对应一个key的时候。在key的后面加上索引即可。例如：
<p><code>
Map<String,Object> params = new HashMap<String, Object>();  
params.put("file0", new File("/sdcard/a.jpg"));  
params.put("file1", new File("/sdcard/a.jpg"));  
params.put("file2", new File("/sdcard/a.jpg"));  

params.put("name", "张三");  
mHttpTools.upload(url, params, httpResult);
</code></p>
这样，三个文件都使用同样的key（file）来上传。

###2.默认开启gzip压缩

###3.支持本地图片（res,asset,sdcard）
BitmapTools的display方法支持各种图片的异步加载
BitmapTools的display方法支持各种图片的异步加载
<p><code>
BitmapTools bitmapTools = new BitmapTools(mContext);  
bitmapTools.display(view, uri);
</code></p>

配置类BitmapDisplayConfig.java。可以配置的有：
默认加载图片，
加载失败图片，
图片尺寸，
加载的动画，
图片圆角属性。
BitmapTools中提供多种方法配置BitmapDisplayConfig，配置过后，BitmapTools将采用该配置来加载显示图片，也可以在display方法中带上配置参数，这种方式不会影响整体配置，只为该次展示图片所使用。
<p><code>
bitmapTools.display(final View view, String uri, BitmapDisplayConfig displayConfig);
</code></p>

###4.diskcache默认使用DiskLruCache

###5.memoryCache默认使用LruCache

###6.request请求添加进度监听（包括上传进度以及加载进度）

###7.允许暂停和继续请求队列
