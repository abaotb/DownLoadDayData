package com.tangb.download;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.tangb.db.JdbcConnect;
import com.tangb.domain.DataJavaBean;
import com.tangb.main.Main;
import com.tangb.utils.Utils;

/**
 * 从网上下载数据
 * 
 * @author tangbao 2015-11-13下午4:10:23
 */
public class DceDownLoad implements Runnable {
	// 第一次加载
	private boolean firstFlag;
	// 日期
	private String dcedate;
	// 交易所地址
	private String dcepath;
	// 数据库对象
	private JdbcConnect dcejdbc;
	// 时间间隔
	private int INTERVAL = 5000;

	public DceDownLoad() {
		super();
	}

	/**
	 * 处理网络内容
	 * 
	 * @param date
	 * 
	 * @param webcontent网络内容
	 * @return javaBean
	 */
	private ArrayList<DataJavaBean> handleWebContent(String webcontent,
			String date) {
		ArrayList<DataJavaBean> dceDataBeanList = null;
		// javabean
		// 郑州
		// String webcontent =
		// dfw.ConnectExchange("http://www.czce.com.cn/portal/DFSStaticFiles/Future/2015/20151112/FutureDataDaily.htm",
		// null);
		// System.out.println(webcontent);
		// webcontent = webcontent.

		Document doc = (Document) Jsoup.parse(webcontent);

		Elements trList = ((Element) doc).getElementsByTag("tr");

		// 当天数据还没出来
		if (trList == null) {
			return dceDataBeanList;
		} else if (trList.size() < 2) {
			return dceDataBeanList;
		}
		 dceDataBeanList = new ArrayList<DataJavaBean>();
		// System.out.println(trList.get(2).toString());
		for (int i = 2; i < trList.size() - 1; i++) { // 第一，二行不要

			Elements tdList = trList.get(i).getElementsByTag("td");
			String text = "";
			if (tdList.size() > 10) { // 说明是一行数据
				text = tdList.get(6).text();
				// String text1 = text.replaceAll("\\s*", "")+"ddd";
				if ("".equals(text)) { // 说明是小计和总计之类的数据
					continue;
				} else {
					// System.out.println(trList.get(i).toString());

				}
				// 将"-"变成0;
				for (Element td : tdList) {
					if ("-".equals(td.text().trim())) {
						td.text("0");
					}
				}

				String exchange = "DCE";
				String contract = tdList.get(0).text().trim()
						+ tdList.get(1).text().trim();
				// String date = "0";
				String prev_Close = "0";// 前收盘

				String open_Pri = tdList.get(2).text().trim();// 开盘价
				String high_Pri = tdList.get(3).text().trim();// 最高价
				String low_Pri = tdList.get(4).text().trim();// 最低价
				String close_Pri = tdList.get(5).text().trim();// 收盘价
				String prev_Settle = tdList.get(6).text().trim();// 前结算
				String settle_Pri = tdList.get(7).text().trim();// 结算价
				String close_Range = tdList.get(8).text().trim();// 涨跌一
				String settle_Range = tdList.get(9).text().trim();// 涨跌二
				String volume = tdList.get(10).text().trim();// 交易量
				String turnover = tdList.get(13).text().trim();// 成交额
				String oI = tdList.get(11).text().trim();// 持仓量

				DataJavaBean cbd = new DataJavaBean(exchange, contract, date,
						prev_Close, prev_Settle, open_Pri, high_Pri, low_Pri,
						close_Pri, settle_Pri, close_Range, settle_Range,
						volume, turnover, oI);
				dceDataBeanList.add(cbd);
			}
		}

		return dceDataBeanList;
	}

	/**
	 * 拼接path
	 * 
	 * @param path
	 * @param param
	 * @return
	 */
	private String jointPath(String path, String date) {
		// String result =
		// "http://www.czce.com.cn/portal/DFSStaticFiles/Future/,2015/20151112,/FutureDataDaily.htm";
		StringBuilder sBuilder = new StringBuilder();
		String[] split = path.split(",");
		sBuilder.append(split[0]);
		// sBuilder.append(date.substring(0, 4));
		// sBuilder.append("/");

		sBuilder.append(date);
		sBuilder.append(split[2]);

		String content = sBuilder.toString();
		sBuilder = null;
		return content;
	}

	/**
	 * 获取数据
	 * 
	 * @param dcepath
	 * @param date
	 */
	private void DceExchange(String dcepath, String date) {
		// System.out.println(czcepath);
		// String[] splitsStrings = czcepath.split("+");

		// 郑州路径
		String path = this.jointPath(dcepath, date);

		// System.out.println(path);

		// 从网上下载数据
		// String webcontentqqqqqqqqqqq =
		// DceDownLoad.ConnectExchange_Post("http://www.dce.com.cn/PublicWeb/MainServlet",
		// "action=Pu00011_result&Pu00011_Input.trade_date=20151112&Pu00011_Input.variety=all&Pu00011_Input.trade_type=0");
		// String webcontent = ddl.ConnectExchange_Get(path);
		String webcontent = Utils.getRequest(path, "gb2312");

		// 去掉特殊字符
		webcontent = webcontent.replace("&nbsp;", "");

		// System.out.println(webcontent);

		// 解析数据
		ArrayList<DataJavaBean> dceDataBean = this.handleWebContent(webcontent,
				date);

		this.HandleJdbc(dceDataBean,"dcedate", dcedate);
	}

	/**
	 * 操作数据库
	 * 
	 * @param dceDataBean
	 */
	private void HandleJdbc(ArrayList<DataJavaBean> dceDataBean,String exchange, String date) {
		if (dceDataBean == null) {
			//System.out.println("DceDownLoad没数据");
			return;
		}
		// for (DataJavaBean dbean : databean) {
		System.out.println("大商所:"
				+ dceDataBean.get(0).date);
		// System.out.println(databean.toString());
		// }
		if (!firstFlag) {
			dcejdbc = new JdbcConnect();
			// 连接数据库
			dcejdbc.connetionDB();
			// 先删
			dcejdbc.deleteDB(dcedate, "DCE");
			// 关闭数据库
			dcejdbc.closeDB();
			firstFlag = true;
		}
		if (dcejdbc == null) {
			dcejdbc = new JdbcConnect();
		}
		// 连接数据库
		dcejdbc.connetionDB();
		// 插入数据库
		dcejdbc.insertDB(dceDataBean);
		// 查询数据库
		// jdbc.queryDB();
		// 关闭数据库
		dcejdbc.closeDB();
		// 修改日期
		String dateString = Utils.addDate(date);
		Utils.writePropertiesFile(exchange, dateString);
	}

	@Override
	public void run() {
		Timer timer = new Timer();
		timer.scheduleAtFixedRate(new TimerTask() {
			public void run() {
				// 日期
				dcedate = Main.properties.getProperty("dcedate");
				// 地址
				dcepath = Main.properties.getProperty("dcepath");
				// 是否是交易日
				if (Utils.isTradingDay("dcedate", dcedate)) {

					DceExchange(dcepath, dcedate);
					try {
						Thread.sleep(2000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}, 1000, INTERVAL);
	}

}
