package com.yonyou.houfei.pack;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.yonyou.houfei.LinuxUtil;

import ch.ethz.ssh2.Connection;

public class LinuxOfDcsTest {
	private static String packPath = "E:\\workspace\\";
	private static String linuxPath = "/usr/local/tomcat-test2/";
	private static String linuxIp = "172.20.32.90";
	private static String linuxUserName = "root";
	private static String linuxPassword = "JMCDMS#test";

	private static List<String> getUploads() { 
		List<String> list = new LinkedList<String>();
//		list.add("dcs.factoryBase,dcsfactorybase.war,tomcat21");
//		list.add("dcs.jmcFinance,dcsjmcfinance.war,tomcat21");
//		list.add("dcs.notify,dcsnotify.war,tomcat21");
//		list.add("dcs.serviceActivity,dcsserviceactivity.war,tomcat21");
//
//		list.add("dcs.claimUsed,dcsclaimused.war,tomcat22");
//		list.add("dcs.partsOrder,dcspartsorder.war,tomcat22");
//		list.add("dcs.techSupport,dcstechsupport.war,tomcat22");
//		list.add("dcs.vehCusView,dcsvehcusview.war,tomcat22");         
//		
//		list.add("dcs.marketSupport,dcsmarketsupport.war,tomcat23");
//		list.add("dcs.targetPrediction,dcstargetprediction.war,tomcat23");
//		list.add("dcs.vehicleSale,dcsvehiclesale.war,tomcat23");
		list.add("dcs.dashboard,dcsdashboard.war,tomcat23");
		
//		list.add("dcs.interface,dcsinterface.war,tomcat24");
		return list;
	}

	public static void main(String[] args) {
		List<String> list = getUploads();
		boolean packStatus = packing(list);
		if (!packStatus) {
			return;
		}
		boolean uploadStatus = uploading(list);
		if (!uploadStatus) {
			return;
		}
		startTomcat(list);
	}

	@SuppressWarnings("static-access")
	private static void startTomcat(List<String> list) {
		try {
			Connection connection = LinuxUtil.getLinuxConnection(linuxIp,
					linuxUserName, linuxPassword);
			while (connection == null) {
				Thread.currentThread().sleep(1000);
				connection = LinuxUtil.getLinuxConnection(linuxIp,
						linuxUserName, linuxPassword);
			}

			SimpleDateFormat nowDateSdf = new SimpleDateFormat("yyyy-MM-dd");
			String nowDateStr = nowDateSdf.format(new Date());
			
			Map<String, String> map = new HashMap<String, String>();
			for (String info : list) {
				String[] infoArr = info.split(",");
				String tomcatName = infoArr[2];
				if(map.containsKey(tomcatName)) {
					continue;
				}
				System.out.println("【" + infoArr[0] + "】 shutdown");
				// shutdown
				LinuxUtil.execCommand1(connection, linuxPath + tomcatName
						+ "/bin/shutdown.sh");
				boolean shutdownSuc = false;
				while (!shutdownSuc) {
					// 监控是否成功shutdown
					String grepRet = LinuxUtil.execCommand1(connection,
							"ps -ef | grep " + tomcatName);
					System.out.println(grepRet);
					if (!grepRet.contains("-Djava.util.logging.config.file="+linuxPath+tomcatName+"/conf/logging.properties")) {
						shutdownSuc = true;
					} else {
						Thread.currentThread().sleep(2000);
					}
				}

				System.out.println("【" + infoArr[0] + "】 startup");
				// startup
				LinuxUtil.execCommand1(connection, linuxPath + tomcatName
						+ "/bin/startup.sh");

				Thread.currentThread().sleep(3000);

				boolean startStatus = false;
				while (!startStatus) {
					ByteArrayOutputStream bos = LinuxUtil.down(connection,
							linuxPath + tomcatName + "/logs/catalina."
									+ nowDateStr + ".log");
					boolean startSuc = checkStartIsSuc(bos);
					if (startSuc) {
						startStatus = true;
						System.out.println("【" + infoArr[0]
								+ "】 startup succss");
					} else {
						Thread.currentThread().sleep(10000);
						System.out.println("【" + infoArr[0]
								+ "】 startuping wait");
					}
				}
				map.put(tomcatName, info);
			}

			connection.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static boolean checkStartIsSuc(ByteArrayOutputStream bos) {
		boolean startSuc = false;
		String[] arr = bos.toString().split("\n");
		int startIdx = 0;
		for (int i = arr.length - 1; i >= 0; i--) {
			if (arr[i]
					.contains("org.apache.catalina.core.StandardEngine.startInternal Starting Servlet Engine")) {
				startIdx = i;
				break;
			}
		}
		for (int i = startIdx; i < arr.length; i++) {
			if (arr[i]
					.contains("org.apache.catalina.startup.Catalina.start Server startup in")) {
				startSuc = true;
			}
		}
		return startSuc;
	}

	@SuppressWarnings("static-access")
	private static boolean uploading(List<String> list) {
		try {
			Connection connection = LinuxUtil.getLinuxConnection(linuxIp,
					linuxUserName, linuxPassword);
			while (connection == null) {
				Thread.currentThread().sleep(1000);
				connection = LinuxUtil.getLinuxConnection(linuxIp,
						linuxUserName, linuxPassword);
			}

			for (String info : list) {
				String[] infoArr = info.split(",");
				System.out.println("【" + infoArr[0] + "】 uploading start");
				boolean uploadStatus = false;
				while (!uploadStatus) {
					// boolean upSuc = LinuxUtil.upload(linuxIp, linuxUserName,
					// linuxPassword, packPath, infoArr[0], infoArr[1],
					// linuxPath, infoArr[2]);
					boolean upSuc = LinuxUtil.upload(connection, packPath
							+ infoArr[0] + "/target/" + infoArr[1], linuxPath
							+ infoArr[2] + "/webapps");
					if (!upSuc) {
						System.out.println("【" + infoArr[0]
								+ "】 uploading [[error]]");
					} else {
						System.out.println("【" + infoArr[0]
								+ "】 uploading [[succss]]");
						uploadStatus = true;
					}
				}
			}
			connection.close();
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}

		return true;
	}

	private static boolean packing(List<String> list) {
		System.out.println("【dcs.common】 packing start");
		String packMsg = LinuxUtil.toPack(packPath, "dcs.common");
		if (!packMsg.contains("BUILD SUCCESS")) {
			System.out.println("【dcs.common】 packing [[error]]");
			return false;
		}
		for (String info : list) {
			String[] infoArr = info.split(",");
			System.out.println("【" + infoArr[0] + "】 packing start");
			boolean packStatus = false;
			while (!packStatus) {
				packMsg = LinuxUtil.toPack(packPath, infoArr[0]);
				if (!packMsg.contains("BUILD SUCCESS")) {
					System.out
							.println("【" + infoArr[0] + "】 packing [[error]]");
				} else {
					System.out.println("【" + infoArr[0]
							+ "】 packing [[succss]]");
					packStatus = true;
				}
			}
		}
		return true;
	}

}
