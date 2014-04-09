package chatClient.Share;

import java.awt.Color;

public interface defineClientVar 
{
	/*broadcast cmd*/
	public final static int CMD_YELL = 0;
	public final static int CMD_POST = 1;
	public final static int CMD_RMMSG = 2;
	public final static int CMD_LEAVE = 3;
	public final static int CMD_KICK = 4;
	public final static int CMD_BEAN_OBJ = 5;
	public final static int CMD_BEAN_CHG = 6;
	public final static int CMD_BEAN_MOV = 7;
	
	public final static String YELL = "/yell";
	public final static String POST = "/post";
	public final static String RMMSG = "/rmmsg";
	public final static String LEAVE = "/leave";
	public final static String KICK = "/kick";
	public final static String BEAN_OBJ = "/obj";
	public final static String BEAN_CHG = "/change";
	public final static String BEAN_MOV = "/mov";
	/*broadcast cmd end*/
	
	/*non-broadcast cmd*/
	public final static int CMD_TELL = 11;
	public final static int CMD_SHOWMSG = 12;
	public final static int CMD_WHO = 13;
	public final static int CMD_CONNECT = 14;
	public final static int CMD_CONNECT_ALREADY = 15;
	
	public final static String TELL = "/tell";
	public final static String SHOWMSG = "/showmsg";
	public final static String WHO = "/who";
	public final static String CONNECT = "/connect";
	/*non-broadcast cmd end*/
	
	/* reply cmd - server */
	public final static int CMD_MSG = 16;
	public final static int CMD_SHOW = 17;
	
	public final static String MSG = "/msg";
	public final static String SHOW = "/show";
	/* reply cmd - server end */
	
	public final static int CMD_UNDEFINE = -1;
	
	/* parse command result */
	public final static int SUCCESS = 1;
	public final static int CLOSE = 0;
	public final static int FAIL = -1;
	/* parse command result end */
	
	/* static msg */
	public final static String USG_CONNECT = "/connect serverName port";
	/* static msg end */
	
	public final static String ERR_SERVER_CANT_CONNECT = "**** The server does not exist.  Please type different domain and/or port. ****";
	public final static String ERR_SERVER_ALREADY_CONNECT = "**** U have already connected server.  Please type command. ****";
	public final static String ERR_DEFAULT_ERR_MSG = "**** Unrecognise msg type ****";
	public final static String getNameMsg = ", welcome to the chat system";
	
	//bean attribute prefix - wbxxx -> it will show on bean editor
	public final static String BEAN_FIELD_PREFIX = "wb";
	
	/*
	 * default attribute value start
	 */
	public final static String BEAN_STRING_DEFAULT_MSG = "String";
	
	public final static int BEAN_REC_DEFAULT_HEIGHT = 100;
	public final static int BEAN_REC_DEFAULT_WEIGHT = 100;
	
	public final static boolean BEAN_JUGGLER_DEFAULT_SETTING = true;
	public final static int BEAN_JUGGLER_DEFAULT_RATE = 150;
	
	public final static int BEAN_CIRCLE_DEFAULT_RADIUS = 100;
	
	public final static Color BEAN_DEFAULT_FORE = Color.WHITE;
	public final static Color BEAN_DEFAULT_BACK = Color.BLACK;
	
	public final static int BEAN_DEFAULT_LEN = 100;
	/*
	 * default attribute value end
	 */
	
	/*
	 * bean attribute type start
	 */
	public final static int BEAN_FIELD_TYPE_STRING = 1;
	public final static int BEAN_FIELD_TYPE_INT = 2;
	public final static int BEAN_FIELD_TYPE_DOUBLE = 3;
	public final static int BEAN_FIELD_TYPE_FLOAT = 4;
	/*
	 * bean attribute type end
	 */
	
	// max bean attribute which start with 'wb'
	public final static int BEAN_DEFAULT_FIELD_NUM = 20;
	
	// RMI task prefix
	public final static String RIM_TASK_FIELD_PREFIX = "RMI";
	
	/* rmi task parameter type start */
	public final static String RMI_TASK_TYPE_INT = "int";
	public final static String RMI_TASK_TYPE_LONG = "long";
	/* rmi task parameter type end */
	
	/* rmi related cmd */
	public final static int CMD_TASK = 18;
	public final static int CMD_REXE = 19;
	public final static int CMD_SHTASK = 20;
	
	public final static String TASK = "/task";
	public final static String REXE = "/rexe";
	public final static String SHTASK = "/showtask";
	
	public final static String USG_TASK = "/task existed_taskName non-duplicated_taskID correct_Args";
	public final static String USG_REXE = "/rexe ur_own_taskName existed_executer(leave empty for server)";
	public final static String USG_SHTASK = "/showtask";
	/* rmi related cmd end */
	
	/* rmi service name start */
	public final static String RMI_NAMING_SERVER_POSTFIX = "@SERVER";
	public final static String RMI_COMPUTE_SERVICE = "COMPUTE";
	public final static String RMI_REGISTER_ADDR = "rmi://127.0.0.1:1099/";
	//public final static String RMI_PI_SERVICE = "PI";
	//public final static String RMI_PRIME_SERVICE = "GridifyPrime";
	/* rmi service name end */
}

