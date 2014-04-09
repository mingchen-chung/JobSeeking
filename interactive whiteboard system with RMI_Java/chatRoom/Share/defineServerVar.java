package chatRoom.Share;

public interface defineServerVar 
{
	/*broadcast cmd - client*/
	public final static int CMD_YELL = 0;
	public final static int CMD_POST = 1;
	public final static int CMD_RMMSG = 2;
	public final static int CMD_LEAVE = 3;
	public final static int CMD_KICK = 4;
	public final static int CMD_BEAN_OBJ = 5;
	public final static int CMD_BEAN_CHG = 6;
	public final static int CMD_BEAN_MOV = 7;
	public final static int CMD_BEAN_SHOW = 8;
	
	public final static String YELL = "/yell";
	public final static String POST = "/post";
	public final static String RMMSG = "/rmmsg";
	public final static String LEAVE = "/leave";
	public final static String KICK = "/kick";
	public final static String BEAN_OBJ = "/obj";
	public final static String BEAN_CHG = "/change";
	public final static String BEAN_MOV = "/mov";
	public final static String BEAN_SHOW = "/beanshow";
	/*broadcast cmd - client end*/
	
	/*non-broadcast cmd - client*/
	public final static int CMD_TELL = 11;
	public final static int CMD_SHOWMSG = 12;
	public final static int CMD_WHO = 13;
	
	public final static String TELL = "/tell";
	public final static String SHOWMSG = "/showmsg";
	public final static String WHO = "/who";
	/*non-broadcast cmd - client end*/

	/* reply cmd - server */
	public final static String MSG = "/msg";
	public final static String SHOW = "/show";
	/* reply cmd - server end */
	
	public final static int CMD_UNDEFINE = -1;
	public final static int MAXCLIENT = 30;
	
	/* static msg */
	public final static String MSG_USERNAME = "Username: ";
	public final static String MSG_WELCOME_1 = "*******************************************\r\n** ";
	public final static String MSG_WELCOME_2 = ", welcome to the chat system.\r\n*******************************************";
	public final static String ERR_NAMECHG_1 = "Error: The user '";
	public final static String ERR_NAMECHG_2 = "' is already online. Please change a name.\r";
	public final static String ERR_NULL_UNAME = "Error: No username is input.";
	public final static String ERR_CMD_UNDEFINE_1 = "**** Your message command '";
	public final static String ERR_CMD_UNDEFINE_2 = "' is incorrect";
	public final static String USG_RMMSG = "/rmmsg msgID";
	public final static String USG_LEAVE = "/leave";
	public final static String USG_KICK = "/kick existedUserName";
	public final static String USG_TELL = "/tell existedUserName msg";
	public final static String USG_SHOWMSG = "/showmsg";
	public final static String USG_WHO = "/who";
	public final static String USG_OBJ = "/obj yourName-newID existedBeanClass posi-x posiy data1 dat2 ...";
	public final static String USG_CHG = "/change yourName-newID data1 data2 ...";
	public final static String USG_MOV = "/mov yourName-newID posi-x posi-y";
	/* static msg end */
	
	/* parse command result */
	public final static int SUCCESS = 1;
	public final static int FAIL = -1;
	public final static int CLOSE = 0;
	/* parse command result end */
	
	public final static String BEAN_FIELD_PREFIX = "wb";
	
	/*
	 * bean attribute type start
	 */
	public final static String BEAN_TYPE_COLOR = "color";
	public final static String BEAN_TYPE_BOOL = "bool";
	public final static String BEAN_TYPE_INT = "int";
	public final static String BEAN_TYPE_DOUBLE = "double";
	public final static String BEAN_TYPE_FLOAT = "float";
	public final static String BEAN_TYPE_STRING = "string";
	
	public final static int BEAN_FIELD_TYPE_COLOR = 1;
	public final static int BEAN_FIELD_TYPE_BOOL = 2;
	public final static int BEAN_FIELD_TYPE_INT = 3;
	public final static int BEAN_FIELD_TYPE_DOUBLE = 4;
	public final static int BEAN_FIELD_TYPE_FLOAT = 5;
	public final static int BEAN_FIELD_TYPE_STRING = 6;
	/*
	 * bean attribute type end
	 */
	/*
	 * RMI related start
	 */
	public final static String RMI_NAMING_SERVER_POSTFIX = "@SERVER";
	public final static String RMI_COMPUTE_SERVICE = "COMPUTE";
	public final static String RMI_REGISTER_ADDR = "rmi://127.0.0.1:1099/";
	//public final static String RMI_PI_SERVICE = "PI";
	//public final static String RMI_PRIME_SERVICE = "GridifyPrime";
	/*
	 * RMI related end
	 */
	
}
