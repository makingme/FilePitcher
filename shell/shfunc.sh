#!/bin/bash
##############################################################################
#	
#	shfunc.sh
#	
#	This script define common Shell functions.
#	
##############################################################################

function findFile
{
        for item in `ls $1/*.jar`
        do
          sum=$sum:${item}
        done
        echo $sum
};

function findDir
{
        for item in `find $1 * | grep ".jar$"`
        do
          sum=$sum:${item}
        done
        echo $sum
};

function  findproc  {
	#파라미터가  1개뿐인  경우,  옵션  출력
	if  ((  $#  ==  1  ))
	then
		echo    " = search PID by "  $1
	fi
	
	PIDS=""
	#PID 위치가 OS별로 다르기 때문에, 케이스별로 나누어서 처리함.
	if [ "$APP_OS" = "OSX" ]
	then
               	ps  $UMS_PS_OPT   |  grep  "$1"  |  grep  -v  grep  |  awk  '{print  $1}{print  $0}' |  while  read  aPID  &&  read  aCmd
		do
            		#자기자신은  제외.
            		if  [[  "$aPID"  !=  "$$"  ]]
                	then
	                	PIDS="$PIDS  $aPID"
        	        	if    ((  $#  <=  1    ))
				then
	                		#파라미터가  1개뿐인  경우,  명령  출력
	                		if  ((  $#  ==  1  ))
					then
						echo  "        #  "$aPID  $aCmd
					fi
	                	fi
                	fi
            	done 
            

	else
	    PIDS=`ps  $UMS_PS_OPT   |  grep  "$1"  |  grep  -v  grep  |  awk  '{print  $2}'`
	fi
}

function  aftercheck  
{
	sleep  1
	PIDS=""
	findproc  $1  s
	if  [[  "$PIDS"  =  ""  ]]
	then
		echo  "프로그램이  시작후  종료되었습니다.  로그를  확인하세요.(pid=  $2  )"
		echo ""
	else
		echo  "프로그램이  정상적으로  시작되었습니다.(pid=  $2  )"
		echo ""

	fi

	if  [  "$SHOWLOG"  =  "on"  ]  
	then
			tail -fn400 $APP_LOGF
	fi
	
	
}

function  killproc  {                        #  kill  the  named  process(es)

	PIDS=""
	findproc  $1

	if  [  "$PIDS"  !=  ""  ]  
	then
		
		if    ((  $#  >  1    ))
		then
			if  [  "$2"  =  "-9"  ]
			then
				echo  kill  -9  $PIDS
				[  "$PIDS"  !=  ""  ]  &&  kill  -9  $PIDS
			else
				echo  kill    $PIDS
				[  "$PIDS"  !=  ""  ]  &&  kill    $PIDS
			fi

			((s=0))
			
			while  ((  s  <  60  ))
			do
				
				findproc  $1  s
				((s=s+1))
				if  [  "$PIDS"  !=  ""  ]
				then
					if  (( s > 1)) 
					then
						echo $UMS_ECHO_OPT  '\033'[2A
					fi
					echo  $s"초  대기..."
					sleep  1
				else
					if  (( s > 1)) 
					then
						echo $UMS_ECHO_OPT  '\033'[2A
					fi
					echo  $s"초  내  모두  종료  확인."
					PIDS=""
					break
				fi
				
			done

			if  [  "$PIDS"  !=  ""  ]
			then
				echo  "강제  종료합니다.(kill  -9)."
				[  "$PIDS"  !=  ""  ]  &&  kill  -9  $PIDS
				
			fi			
	
		else
			echo  "kill  $PIDS  ?(y|n)"
			read  yn
			if  [    $yn  =  "y"  ]
			then
				[  "$PIDS"  !=  ""  ]  &&  kill  $PIDS
				
				findproc  $1  s
				((s=0))
				
				while  ((  s  <  10  ))
				do
					
					findproc  $1  s
					((s=s+1))
					if  [  "$PIDS"  !=  ""  ]
					then
						echo  $s"초  대기..."
						sleep  1
					else
						echo  $s"초  내  모두  종료  확인."
						PIDS=""
						break
					fi
					
				done

				if  [  "$PIDS"  !=  ""  ]
				then
					echo  "강제  종료합니다.(kill  -9)."
					[  "$PIDS"  !=  ""  ]  &&  kill  -9  $PIDS
					
				fi
	
			else
				exit  0;
				
			fi
		fi

	else
		echo  "프로세스가  없습니다."
	fi	
}


function  runutil  {
	
	PIDS=""
	if  [  "$3"  =  "log"  ]  
	then
		SHOWLOG="on" ;
	else
		SHOWLOG="off" ;
	fi
	
	findproc  $1  s
	

	echo  " = log file  "  $APP_LOGF
	
	if  [  "$2"  =  "conf"  ]  
	then
	  echo "$APP_PNM 관련설정 확인."
	  exit ;
	fi
		
	if  [  "$2"  =  "log"  ]  
	then
		if  [  "$3"  !=  ""  ]
		then
			tail -fn$3 $APP_LOGF
			exit ;
		else
			tail -f $APP_LOGF
			exit ;
		fi
	fi
	
	
	if  [  "$2"  =  "stop"  ]  
	then
		if  [  "$PIDS"  !=  ""  ]
		then
			killproc  $1  $3
			exit  
		else
			echo  "실행중인  $APP_PNM 프로그램이  없습니다. $0"
			exit
		fi
	fi
	
	
	if  [  "$2"  =  "restart"  ]  
	then
		if  [  "$PIDS"  !=  ""  ]
		then
			killproc  $1  $3  
			return ;
		else
			return ;
  
		fi
	fi
	
	if  [  "$2"  =  "kill"  ]  
	then
		if  [  "$PIDS"  !=  ""  ]
		then
			killproc  $1  f
			exit  
		else
			echo  "실행중인  $APP_PNM 프로그램이  없습니다. $0"
			exit
		fi
	else
	
		if  [  "$PIDS"  !=  ""  ]
		then
	      		echo  "$APP_PNM 프로그램이  이미  실행중입니다.PIDS="  $PIDS

	      		echo ""
	            		exit
	            	fi
	fi

    if  [  "$2"  =  "chkproc"  ]
    then
            if  [  "$PIDS"  !=  ""  ]
            then
                    echo  "$APP_PNM은 실행중입니다.  $0"
                    echo ""
                    exit
            else
                    echo  "실행중인  $APP_PNM 프로그램이  없습니다.  $0"
                    if  [  "$3"  ==  ""  ]
                    then
                            echo "실행중인  $APP_PNM 프로그램이  없습니다."
                             
                            exit 1
                    else
                            echo ""
                            $3
                            exit 1
                    fi
            fi
    fi
	
	if  [  "$2"  !=  "start"  ]  
	then
		if  [  "$PIDS"  !=  ""  ]
		then
			echo  "$APP_PNM은 실행중입니다.  $0"
			echo ""
			exit  
		else
			echo  "실행중인  $APP_PNM 프로그램이  없습니다.  $0"
			echo ""
			exit
		fi
		
# start the program
	else
		echo "" >> $APP_LOGF
		echo ">>>>>>>>>>>>>>>>>>>> START >>>>>>>>>>>>>>>>>>>>>" >> $APP_LOGF
	fi
}