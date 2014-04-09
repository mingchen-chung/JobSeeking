#!/usr/bin/perl

use strict;

use POE;
use POE::Component::IRC;
use Class::Struct;

#************************************
# the struct to remember channel members' info
#************************************
struct Member =>
{
	name=>'$',
	status=>'$', # 0 - wait, 1 - fighting
};

#************************************
# variables for global use
#************************************

#************************************
# member status
#************************************
my $WAIT = 0;
my $FIGHTING = 1;

#************************************
# necessary msg
#************************************
my $LOSE = "LOSER";
my $WIN = "WINNER";
my $FIGHT = "FIGHT";
my $Ident = 'IDENTIFY';

#************************************
# set necessary parameters from argv
#
# ex: perl referee.pl YOUR_NICK_NAME IDENTIFY_PASSWD TESTING_CHANNEL
#
# need IDENTIFY_PASSWD to identify ur account, because u need channel OP to kick loser
#************************************
my $Nick = $ARGV[0]; 
my $Passwd = $ARGV[1]; 
my $AutoJoinChannel = "##".$ARGV[2]; 

#************************************
# set server information
#************************************
my $NickServ = 'NickServ';
my $IRCServer = "irc.freenode.net";
my $IRCPort = 6667;

#************************************
# array to save member info
#************************************
my @gMemberList;

#************************************
# array to save operator list
#************************************
my @Operator = ($Nick); 

die "Usage: perl Hwk1_TA_BOT.pl YOUR_NICK_NAME IDENTIFY_PASSWD TESTING_CHANNEL\n" if(@ARGV != 3);

my $irc = POE::Component::IRC->spawn();
#************************************
# register event and related handler
#************************************
POE::Session->create
( 
	inline_states => 
	{
		_start     => \&irc_start,
		irc_376    => \&irc_connect,  #end of motd
		irc_353    => \&irc_names,
		irc_ctcp_action=> \&irc_me,
		irc_join   => \&irc_join,
		irc_nick   => \&irc_nick,
		irc_part   => \&irc_part,
		irc_quit   => \&irc_part,
		irc_public => \&irc_pub_msg, 
	}
);

#************************************
# send_fight_msg 
#
# use to send "OPP_NAME FIGHT RANDOM_SEED" msg to fighting user pair
#
# print "==== index = $i  count = $count.... => this is for debugging
#************************************
sub send_fight_msg
{
	return if($#gMemberList < 1);

	my $count = 0;
	my @player = ();
	my $range = 65535;
	my $i = 0;

	foreach(@gMemberList)
	{
		print "==== index = $i  count = $count  status = ".$_->status."  name = ".$_->name." Queue len = $#gMemberList\n";
		if($count < 2 && $_->status == $WAIT)
		{
			push @player,$_->name;
			$_->status($FIGHTING);
			$count++;
		}
		if($count == 2)
		{
			$irc->yield(privmsg=>$AutoJoinChannel=>$player[0].': '.$FIGHT.' '.$player[1].' '.int(rand($range)));
			$irc->yield(privmsg=>$AutoJoinChannel=>$player[1].': '.$FIGHT.' '.$player[0].' '.int(rand($range)));
			@player = ();
			$count = 0;
		}
		$i++;
	}

	if($count == 1)
	{
		my $recoverName = pop @player;

		foreach(@gMemberList)
		{
			$_->status($WAIT) if($_->name eq $recoverName);
		}
	}
}

#************************************
# member_list_gen
#
# generate channel member list to maintain member info
#************************************
sub member_list_gen
{
	my @MemberList = split(/ /, $_[0]);

	for(my $i = 0 ; $i <= $#MemberList ; ++$i)
	{
		$MemberList[$i] =~ s/[@]?(.*)/$1/;

		next if grep{$MemberList[$i] eq $_} @Operator;
		&member_list_update($MemberList[$i]);
	}
}

#************************************
# delete_from_list
#
# when one user leaves or be kicked, delete this member info from member list
#************************************
sub delete_from_list
{
	my $deleteMember = $_[0];

	for(my $i = 0 ; $i <= $#gMemberList ; $i++)
	{
		next if($gMemberList[$i]->name ne $deleteMember);

		if($gMemberList[$i]->name eq $deleteMember)
		{
			delete $gMemberList[$i];
			&shift_List($i);
			return;
		}
	}
}

#************************************
# member_list_update
#
# 1. check user is Operator or not, if it is Operator, this user shouldn't join fighting process
#
# 2. if user is not in gMemberList array, push it to this array
#************************************
sub  member_list_update
{
	my $joinNick = $_[0];
	my $bFound = 0;

	return if grep{$joinNick eq $_} @Operator;

	for(my $i = 0 ; $i <= $#gMemberList ; $i++)
	{
		next if($gMemberList[$i]->name ne $joinNick);

		if($gMemberList[$i]->name eq $joinNick)
		{
			if($#_ == 1)
			{
				$gMemberList[$i]->name($_[1]);
				return;
			}
			$bFound = 1;
			last;
		}
	}
	push @gMemberList, Member->new(name => $joinNick, status => $WAIT) if($bFound == 0);
}

#************************************
# shitf_List
#
# because delete array element will remain one empty element, 
#
# this program will behave oddly, so shift forward
#************************************
sub shift_List
{

	my $index = $_[0];
	my $i;

	for($i = $index ; $i < $#gMemberList ; $i++)	
	{
		$gMemberList[$i] = $gMemberList[$i+1];
	}
	delete $gMemberList[$i];
}

#************************************
# msg_decision
#
# decide how to deal "WINNER" and "LOSER" msg from public or ctcp_action
#
# WINNER: set status to 1 ($WAIT), and wait for next opponent
#
# LOSER: kick and delete from gMemberList
#************************************
sub msg_decision
{
	my $nick = $_[0];
	my $msg = $_[1];

	if($msg eq $LOSE)
	{
		&delete_from_list($nick);
		$irc->yield(kick=>$AutoJoinChannel=>$nick=>"Loser! KICK!");
		print "==== $nick $msg \n";
		&send_fight_msg();
	}
	elsif($msg eq $WIN)
	{
		foreach(@gMemberList)
		{
			if($nick eq $_->name)
			{
				$_->status($WAIT);
				print "==== $nick $msg ->".$_->status."\n";
				&send_fight_msg();
				last;
			}
		}
	}
}

#************************************
# irc_start
#
# register event I interested and connect to server
#************************************
sub irc_start 
{
	$irc->yield(register=> qw(_start 376 mode 353 ctcp_action join public part quit));
	$irc->yield
	(
		connect=> 
		{ 
			Nick     => $Nick,
			Username => $Nick,
			Ircname  => $IRCServer,
			Server   => $IRCServer, 
			Port     => $IRCPort,
		}
	);
}

#************************************
# irc_connect
#
# first line is used to identify ur account
#
# second line is used to join the channel u assigned
#
# third line is used to generate random seed
#************************************
sub irc_connect 
{
	$irc->yield(privmsg=>$NickServ=>$Ident.' '.$Passwd);
	$irc->yield(join=>$AutoJoinChannel);
	srand(time() ^ $$);
}

#************************************
# irc_names
#
# generate gMemberList from /names event
#************************************
sub irc_names 
{
	my $names = (split /:/, $_[ARG1])[1];

	&member_list_gen($names);
}

#************************************
# irc_nick
#
# when someone change it's nick, update gMemberList
#************************************
sub irc_nick 
{
	my $oldNick = (split /!/, $_[ARG0])[0];
	my $newNick = $_[ARG1];

	&member_list_update($oldNick, $newNick);
	print "### $oldNick is now known as $newNick\n";
}

#************************************
# irc_part
#
# when someone leaves, delete it from gMemberList
#************************************
sub irc_part 
{
	my $nick = (split /!/, $_[ARG0])[0];

	&delete_from_list($nick);
	print "### $nick has leaved\n";
}

#************************************
# irc_join
#
# when someone join this channel, update gMemberList,
#
# and try to send FIGHT msg to it
#************************************
sub irc_join 
{
	my $nick = (split /!/, $_[ARG0])[0];
	my $channel = $_[ARG1];

	if($nick ne $Nick && $channel eq $AutoJoinChannel)
	{
		&member_list_update($nick);
		print "### $nick has joined $channel\n";
		&send_fight_msg();
	}
}

#************************************
# irc_pub_msg and irc_me
#
# do related action about channel msg
#************************************
sub irc_pub_msg
{
	my $nick = (split /!/, $_[ARG0])[0];
	my $channel = $_[ARG1]->[0];
	my $msg = $_[ARG2];

	&msg_decision($nick, $msg);
}

sub irc_me
{
	my $nick = (split /!/, $_[ARG0])[0];
	my $msg = $_[ARG2];

	&msg_decision($nick, $msg);
}

#start everything
$poe_kernel->run();
exit 0;
