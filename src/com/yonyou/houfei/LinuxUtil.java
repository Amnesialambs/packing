package com.yonyou.houfei;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;

import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.SCPClient;
import ch.ethz.ssh2.Session;
import ch.ethz.ssh2.StreamGobbler;

public class LinuxUtil {
	public static String toPack(String packPath, String packDir) {
		String retStr = "";
		try {
			InputStream fis = null;
			InputStreamReader isr = null;
			Process p = Runtime.getRuntime()
					.exec("cmd /c cd " + packPath + packDir
							+ " && mvn clean install");

			fis = p.getInputStream();
			isr = new InputStreamReader(fis);
			BufferedReader br = new BufferedReader(isr);
			StringBuilder sb = new StringBuilder();
			String str = "";
			while ((str = br.readLine()) != null) {
				sb.append(str);
			}
			retStr = sb.toString();
			p.destroy();
			br.close();
			isr.close();
			fis.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return retStr;
	}

	public static boolean upload(String linuxIp, String linuxUserName,
			String linuxPassword, String packPath, String packDir,
			String packName, String linuxPath, String toLinuxTomcat) {
		boolean upSuc = false;
		try {
			// 1, 创建一个连接connection对象
			Connection conn = new Connection(linuxIp);
			// 2, 进行连接操作
			conn.connect();
			// 3, 进行连接访问授权验证
			boolean isAuth = conn.authenticateWithPassword(linuxUserName,
					linuxPassword);
			if (!isAuth) {
				System.out.println("Authentication failed");
			} else {
				upSuc = true;
				System.out.println("Authentication succ");
			}
			if (!upSuc) {
				return upSuc;
			}
			// 4, 创建一个SCPClient对象
			SCPClient client = new SCPClient(conn);
			client.put(packPath + packDir + "/target/" + packName, linuxPath
					+ toLinuxTomcat + "/webapps");
			conn.close();

			upSuc = true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return upSuc;
	}
	
	public static Connection getLinuxConnection(String linuxIp, String linuxUserName,
			String linuxPassword) {
		Connection connection = null;
		try {
			// 1, 创建一个连接connection对象
			connection = new Connection(linuxIp);
			// 2, 进行连接操作
			connection.connect();
			// 3, 进行连接访问授权验证
			boolean isAuth = connection.authenticateWithPassword(linuxUserName,
					linuxPassword);
			if (!isAuth) {
				connection = null;
				System.out.println("Authentication failed");
			} else {
				System.out.println("Authentication succss");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return connection;
	}
	
	public static boolean upload(Connection connection, String windowFile, String linuxPath) {
		boolean upSuc = false;
		try {
			// 创建一个SCPClient对象
			SCPClient client = new SCPClient(connection);
			client.put(windowFile, linuxPath);
			upSuc = true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return upSuc;
	}
	
	public static ByteArrayOutputStream down(Connection connection, String linuxFile) throws IOException {
//		SFTPv3Client sftpClient = new SFTPv3Client(connection);
//		SFTPv3FileHandle handle = sftpClient.openFileRW("");
//		handle.
		SCPClient client = new SCPClient(connection);
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		client.get(linuxFile, os);
		return os;
	}
	
	public static String execCommand1(Connection connection, String execStr) throws IOException {
		Session session = connection.openSession();
		session.execCommand(execStr);
		InputStream is = new StreamGobbler(session.getStdout());
		InputStreamReader isr = new InputStreamReader(is);
		BufferedReader br = new BufferedReader(isr);

		StringBuilder sb = new StringBuilder();
		String str = "";
		while ((str = br.readLine()) != null) {
			sb.append(str).append("\n");
		}
		System.out.println("ExitStatus:" + session.getExitStatus());

		session.close();
		br.close();
		isr.close();
		is.close();
		return sb.toString();
	}
	
	public static List<String> execCommand2(Connection connection, String execStr) throws IOException {
		Session session = connection.openSession();
		session.execCommand(execStr);
		InputStream is = new StreamGobbler(session.getStdout());
		InputStreamReader isr = new InputStreamReader(is);
		BufferedReader br = new BufferedReader(isr);

		List<String> list = new LinkedList<String>();
		String str = "";
		while ((str = br.readLine()) != null) {
			list.add(str);
		}
		System.out.println("ExitStatus:" + session.getExitStatus());

		session.close();
		br.close();
		isr.close();
		is.close();
		return list;
	}
}
