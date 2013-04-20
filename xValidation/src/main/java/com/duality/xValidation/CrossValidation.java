package com.duality.xValidation;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;

import com.duality.server.openfirePlugin.InstanceLoader;
import com.duality.server.openfirePlugin.dataTier.CachingHistoryDbAdapter;
import com.duality.server.openfirePlugin.dataTier.HistoryDatabaseAdapter;
import com.duality.server.openfirePlugin.dataTier.HistoryEntry;
import com.duality.server.openfirePlugin.dataTier.MessageType;
import com.duality.server.openfirePlugin.dataTier.NextHistoryInfo;
import com.duality.server.openfirePlugin.dataTier.SqliteDbAdapter;
import com.duality.server.openfirePlugin.prediction.FeatureKey;
import com.duality.server.openfirePlugin.prediction.PredictionEngine;
import com.duality.server.openfirePlugin.prediction.impl.feature.AtomicFeature;
import com.duality.server.openfirePlugin.prediction.impl.feature.AtomicFeaturesManager;
import com.duality.server.openfirePlugin.prediction.impl.feature.AtomicFeaturesProvider;
import com.duality.server.openfirePlugin.prediction.impl.feature.TfIdfKey;
import com.duality.server.openfirePlugin.prediction.impl.feature.TokenFeaturesProvider;
import com.duality.server.openfirePlugin.prediction.impl.store.FPStore;
import com.duality.server.openfirePlugin.prediction.impl.store.TfIdfFeatureStore;
import com.duality.server.openfirePlugin.prediction.impl.store.TfIdfStore;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multiset.Entry;
import com.google.common.collect.Sets;

public class CrossValidation {

	static final int N_FOLD = 10;
	private static final long MAX_DEAD_AIR_INTERVAL = 60 * 60 * 1000L;
	private static final String INSERT_TEST_SQL = "INSERT INTO xValidation_test (name) VALUES (?)";
	private static final String INSERT_QUERY_SQL = "INSERT INTO xValidation_query (query, actual) VALUES (?, ?)";
	private static final String INSERT_PREDICTION_SQL = "INSERT INTO xValidation_prediction (query_id, test_id, char_given, rank, prediction) VALUES (?, ?, ?, ?, ?)";
	private static final String ID_SQL = "SELECT last_insert_rowid()";

	private static final Set<Query> QUERYS = Sets.newHashSet(
			new Query("t", "Ok!"),
			new Query("nei bin gor time slot?", "ar..."),
			new Query("我諗中國同美國有D唔同", "我都知會唔同...我想知ust有無人去...宜家好擔心哩..."),
			new Query("r u here?", "Yup"),
			new Query("haha gum jou great great la", "我地幾時可以見下"),
			new Query("我今個星期要玩緊個比賽，超級忙，所以先唔得閒寫", "Easy la gum. Hardcode quedtion. Send me the thing la! 但係我得呢兩個得閒"),
			new Query("Found someone to eat already haha", "ð"),
			new Query("Ok!", "Thx la"),
			new Query("幾多", "100"),
			new Query("so complicated lol", "佢話camary識左tony之後就變本加厲"),
			new Query("thrusday LG7 tea?", "thur day off wor:("),
			new Query("上堂講得快到冇人有...d assignment好難好chur有source都冇用仲份份要通頂...但係個grade係合理既", "下個sem仲有科networking都係佢教 /.\\"),
			new Query("上完堂問", "我岩岩copy左個fyp report instructions 去 Google docs, 要寫6頁紙啊！！"),
			new Query("Having lesson", "till 6.20"),
			new Query("个有兩個月", "洗唔洗咁寸"),
			new Query("咁就唔係中東啦", "哈"),
			new Query("八月？定五月？", "八月"),
			new Query("we should email him to see", "u get the second guy's phone number?"),
			new Query("oh...", "stupid ust"),
			new Query("ccp", "dc"),
			new Query("Today 5:15 that one", "its a present only ar but i will go"),
			new Query("im in dual rm. i will wait jack a while ar. LG7?", "ok"),
			new Query("around1920 la", "np!"),
			new Query("影左相睇下", "see p454 of book pdf"),
			new Query("食咩 lg7?", "係扶手電梯旁邊"),
			new Query("how bout u", "Y"),
			new Query("haha", "deadlinesss"),
			new Query("u trust the seller here??", "u biy battlefield they may give u call of duty 1 lol"),
			new Query("btw你住hall幾?", "6"),
			new Query("she is a dualer...", "nei ng ho jor chi d..."),
			new Query("where r u lol", "啱啱好"),
			new Query("好凍好難打字", "去樂園玩鬼屋"),
			new Query("i think i will try find some solid fuel", "and i will be done"),
			new Query("too", "個club成年就幫你做一個project"),
			new Query("Weka個ib1無講點measure distance, 我信自己", "im wrong"),
			new Query("U do one i do one", "Then staple them tgt"),
			new Query("記得好似係both", "我見佢website 寫一年"),
			new Query("wow", "u get wtsapp!!!"),
			new Query("anything need to prepare?", "when r u free?"),
			new Query("XDD", "跟隊"),
			new Query("2-5，有3堂", "又係時候出去踩單車啦=____="),
			new Query("其實q2最難", "我覺得"),
			new Query("Free at 1430? we can meet at dual room", "ok"),
			new Query("我已經行唔到路啦", "攰到落樓梯差啲跪低，真係唔做運動都唔知自己虧"),
			new Query("ya just then", "Omg....."),
			new Query("即日book好似無", "Yau before"),
			new Query("gum ngo today do minutes for the report sin", ""),
			new Query("我心諗..", "what can i say worrrr"),
			new Query("shouldnt it be at the middle somewhere?", "ar stupid jor haha"),
			new Query("haha gd nitw", "not yet sleep-0-?!"),
			new Query("No record in the live cd ga", "No trace "),
			new Query("haha", "and it worked right?"),
			new Query("yes.", "jacq said only core wo"),
			new Query("busi course", "broken grade ga la"),
			new Query("Dou hai ng lunch laa", "See u all at 2"),
			new Query("Ys", "ic..."),
			new Query("No... At office", "But I am hungry..."),
			new Query("-,-", "Ho la"),
			new Query("永遠樂觀的Kenny haha", "我唔想做"),
			new Query("No ar", "Juz google chrome"),
			new Query("talk tonite la", "talk tonite la"),
			new Query("but none of the rest hv lpl", "ya"),
			new Query("Engine 60%", "holy..."),
			new Query("kai左lol", "我好開心呀"),
			new Query("peter", "still finding"),
			new Query("Bausch & lomb is no good xd", "咦你唔係要呢個？！我記錯左-_-"),
			new Query("咁六月幾多working days", "番幾多日"),
			new Query("係咪少少肥仔??XD", "唔係~我知道你講邊個"),
			new Query("Just text him the amount", "Thomas came to singapore"),
			new Query("好焗促喎", "Good air con systemð"),
			new Query("Ci sin", "Lol"),
			new Query("Kill all of u", "Nooooo"),
			new Query("willing to teach n help wor", "ya i heard sth similar"),
			new Query("我戒左牛肉好耐", "Pity"),
			new Query("I needa move", "Cant remain still"),
			new Query("", "did u read news for recent market? they might ask wo"),
			new Query("I don't know the singapore number la", "Hahaha"),
			new Query("Fiona~~", "你個potential target 其實我識唔識?"),
			new Query("Sorry i thought i replied u at the airport><", "Yes the 2nd one as well"),
			new Query("lol", "and poor catherine"),
			new Query("today yau mo fyp neeting?", "no.la"),
			new Query("wow so lucky u", "which place ar?"),
			new Query("not yet back ust?", "coming back.."),
			new Query("Hagahaha", "lol"),
			new Query("but no little talks lo so many ppl", "ð"),
			new Query("Mb nights", "fri,sat,sun"),
			new Query("揾日reu去潮聖 哈哈", "(throw up)"),
			new Query("Tell u tmr:)", "okð"),
			new Query("???", "去邊？"),
			new Query("For half day", "Coz my cousin is leaving hk"),
			new Query("佢好似想我入佢間公司~我唔想~就完全冇應佢~", "Hahahahahaha"),
			new Query("Errr ng g yau mo", "Help i find find la"),
			new Query("y me only:(?!", "get any gd target now?"),
			new Query("同埋做嘢唔chur", "Hahahaha"),
			new Query("Jo gun mud?", "Da gay la"),
			new Query("I have been making up but you know I cannot make so many data up", "Why stockholm lol?"),
			new Query("Use absinthe", "Should i jb actually??"),
			new Query("我頭先有諗過洗唔洗打多次哈哈", "ok la"),
			new Query("ng go see doctor dou g when illness i got la usually", "咁我個人虧成日都小病..小病是福呀"),
			new Query("With whom", "Ngo mo lei yau sau ng dou wind"),
			new Query("tom cruise n brad pitt!!", "whc"),
			new Query("Haha why?", "Coz my cousin is leaving"),
			new Query("ð", "i saw manning"),
			new Query("睇下有幾大隻", "Yesyesyes"),
			new Query("ð", "Okok:("),
			new Query("係咪你講嗰個呢??", "Yes la"),
			new Query("wtf", "wtf"),
			new Query("Coz my cousin is leaving", "Going bk to ca"),
			new Query("如果有其他酒我真係有客介紹喎", "上次支白酒其實係我屋企搬番黎架ð"),
			new Query("ð", "ð"),
			new Query("yes r, always this sat", "Totally forgot"),
			new Query("Yes", "If cancelled in the morning?"),
			new Query("Hahaha nice afternoon activity", "If signal cancelled before 1pm tomorrow, call me"),
			new Query("We are at uni bar", "頂你地"),
			new Query("I m chatting wf non jupas freshmen", "Do things beautiful then i dnt pen u"),
			new Query("Juz 10", ":P"),
			new Query("Save him for u ð", "annie is mine"),
			new Query("我reg左hall camp但係而家番工個老師唔比我請假點算ð", "因為原本有四個人   三個cu 我ust  佢地個兩日有introduction dayð¥ 得番我"),
			new Query("呢部機既我俾你免費試ð", "ð"),
			new Query("So fake ma", "Ended la"),
			new Query("Me dislike them .\\  /.!!!", "haha, two more days and free la"),
			new Query("真係要早啲話我知...", "U told them?"),
			new Query("果日得唔得", "Can i confirm u tmr?"),
			new Query("甘我危坐窗邊ð", "Remember to bring ur phone arrr"),
			new Query("Leo?", "係我正囉ð"),
			new Query("Im inside the station", "女弓木"),
			new Query("番工先飲紅白ð", "苦ð"),
			new Query("Forgot to save", "u fb him"),
			new Query("Osyter??", "1kg how many??"),
			new Query("蛋糕okay喎又！", "哈"),
			new Query("ya..very weird..", "Got chrome!!"),
			new Query("When?", "I wanna go too ð\nLife is so dull in singapore"),
			new Query("aiya", "just hv fun"),
			new Query("ticket", "Movie with lang lui?!ð"),
			new Query("如果星期二3-3:45 同星期三1-1:45 係咪兩個時間都得", "我唔記得左個學生約左邊一段時間"),
			new Query("ok!!", "When is ur lunch break?"),
			new Query("cuð", "媽媽都有既ð"),
			new Query("搞開既輕鬆啦ð", "ð"),
			new Query("So practical hahaha", "haha"),
			new Query("哈哈", "你宜家都搵到我啦"),
			new Query("Thx:)", "sosad ytd buy ma"),
			new Query("just drinking and hv fun je", "nth bad ge"),
			new Query("要買幾多？", "未sure我拎到幾多盒"),
			new Query("Hahaha", "Help me ask the boys go club"),
			new Query("小心身體！hahahað", "搞開既輕鬆啦ð"),
			new Query("Remember to bring ur phone arrr", "XS"),
			new Query("He occupies most of her free time as well!!", "I wanna go swim"),
			new Query("聽日都見到啦", "plz plz plz"),
			new Query("find me at dim siu2", "waiting for u"),
			new Query("This js insane", "Is*"),
			new Query("mobile", "on9"),
			new Query("屋企食ja", "唔通你開鋪咩ð"),
			new Query("Only my and annie bb now!!", "Me*"),
			new Query("Chi sin x infinity", "This js insane"),
			new Query("要我扮cute???ð", "I know you are not"),
			new Query("y u dun ask le", "Ok"),
			new Query("係好飽架真係", "我朝早食唔到太多ð"),
			new Query("no more?", "Spent some time walking ard cwb then go bk dinner la"),
			new Query("條眼線仲係度！唔驚！", "hahaha"),
			new Query("i try la", "Juz walk out of the office no one will notice lol"),
			new Query("O...", "Gum tmr sin find u talk"),
			new Query("Half course abt relativity!!", "Wow! Great!!!!"),
			new Query("Yes", "About to sleep"),
			new Query("會唔會無架去到？", "May be"),
			new Query("Similar lor gum", "Aiiii"),
			new Query("係個fd度訓", "佢今晚咁岩唔係度"),
			new Query("未搶晒", "core都無位"),
			new Query("Betty did this?!", "Omg"),
			new Query("But i'll live with math if its for phy", "Told u ga la! Haha"),
			new Query("Them", "Going back to hall"),
			new Query("如果見到佢幫你問下", "咁無野啦"),
			new Query("See u", "Good 5/10 見"),
			new Query("Me doing my own part lol", "Just back home"),
			new Query("Lol", "I like ur conversation lol"),
			new Query("Ha, u always hungry ga la", "No lor"),
			new Query("Oh sorry", "Juz saw ur msg"),
			new Query("你都take左4611 ar?", "Core ar"),
			new Query("加粒鹽夠哂", ".."),
			new Query("Definitely wanna eat right mow", "So hungry"),
			new Query("佢resign左", "ð"),
			new Query(":(", "如果你平時同佢okay應該無野既"),
			new Query("佢話想溫書", "佢忍唔到唔玩啦ð"),
			new Query("我都冇咁早瞓", "可能唔返屋企"),
			new Query("They are all so stupid and slow", "And 拖泥帶水"),
			new Query("有車未？", "上左車啦"),
			new Query("餐", "屋企食面"),
			new Query("Was chatting with her ma", "Be friends l"),
			new Query("Made for chris as well lr", "Lor"),
			new Query("Um", "Inside la"),
			new Query("Yes", "Can u get in?"),
			new Query("可以", "230?"),
			new Query("We did nth lol", "We did nothing too. But I believe gen and William hv research many things on their own"),
			new Query("Ohyaya", "Ask ur roommate not to lock the door tonight"),
			new Query("其實佢地有無plan先？", "可能佢地係幫到下下莊陪通頂呢part~"),
			new Query("Oh no", "Sjould be"),
			new Query("Curious mað", "dim wai zi failð"),
			new Query("Han hau!", "Go home quicker and cheaper wor"),
			new Query("Juz in time!!", "Happy birthday rabbit!!:)"),
			new Query("Right?", "Ngngng"),
			new Query("Hai la hai la ho shiny laaaa", "ð"),
			new Query("Ok ar call u then", "Or you want me send my homework to you?"),
			new Query("無計架喎ð", "你叫到呀澄返我就返"),
			new Query("Hey do u know any dress code tmr?", "Our team will do smart casual"),
			new Query("Subcom今晚開大會", "睇黎你好大機會要危險一下ð"),
			new Query("Piano tmr at 3pm saiwan as usual???", "Right"),
			new Query("令女", "哈"),
			new Query("They did!", "reli? i didnt erite it down then..."),
			new Query("I should be busy this week but not the days before 29th", "ð"),
			new Query("Then u can't get a offer", "I dnt think airport needs a mascot"),
			new Query("Yup", "Do u know who is?"),
			new Query("Lol\nBut the other 2 are not ipo ga", "Make sense ar"),
			new Query("I hate fighting with gbus ppl in discussions", "Nope"),
			new Query("ð", "No"),
			new Query("Oh god yes!", "I swear if our boss is like her..."),
			new Query("Coming out from hall6", "我啱啱上小巴"),
			new Query("Should be about the game", "Same"),
			new Query("Walking luffy", "Haha ok"),
			new Query("Thxxx", "Any offer received?"),
			new Query("Back sleep tonight?", "No ar go home"),
			new Query("maisie said she just arranged bbq with other fds that day", "no car lu..."),
			new Query("So maybe night dinner plus escape la", "ok!"),
			new Query("Send to me la give u back on thur", "Ywlam@ust.hk"),
			new Query("其實都好少事姐", "個組爸超級口賤"),
			new Query("Ok", "Find you in hall2concourse"),
			new Query("Terry said he will print, please confirm", "C file no change"),
			new Query("ok!", "What is the email title requirement???!"),
			new Query("ð", "好想表姐同佢絕交ð"),
			new Query("Fb", "And the .c file?"),
			new Query("What is in tmr?!", "I mean thursday"),
			new Query("之後我話以後唔理佢", "佢就退左gp..."),
			new Query("有聽d n i know he wilk hv concert during christmas", "想搵人去睇ð­"),
			new Query("Haha I think she doesn't know we three can chat tgt in fb", "Oh! Actually I don't have to go to school already"),
			new Query("Tonight have recruitment event..come back after dinner", "我放棄"),
			new Query("Then now doing hw..", "Just did office work for 6 hours"),
			new Query("Lol", "What did u say then?"),
			new Query("thanks!!", ":D!np!"),
			new Query("Oh it was not an i interview..just lunch with a guest but it was great:)", "I am not free on saturday la..interview in afternoon and i want to prepare in the morning"),
			new Query("Ngo mo ð", "Im year4-.-"),
			new Query("ð", "沖涼then訓"),
			new Query("Call her tmr", "O..."),
			new Query("haha系啦系啦ð", "你星期一約左我架"),
			new Query("ð", "Longggg week.sigh"),
			new Query("夜晚？你幾點收工？", "Tue 1730 at wanchai"),
			new Query("Thx la:)", ":)))"),
			new Query("我send 左份source", "你個份唔知漏乜，你比較下ð"),
			new Query("Not workingg on iphone", "I didnt try ar"),
			new Query("Do u know which rm rita is in??", "She is not on wtsapp"),
			new Query("Lets 24 la", "She ask 200 duc ng duc"),
			new Query("Good luck to both of us!", "Lol"),
			new Query("Basically it seems to me that they aren't as willing to give out money as before", "Wtf"),
			new Query("Where ar u?", "Movie"),
			new Query("睇完會係覺得你想同佢吹水", "我們直都諗見面講"),
			new Query("Still sounds gay", "Lol"),
			new Query("Yes", "Ur fds r coming eif u?"),
			new Query("Very far... But it's too late to eat it right now", "Two days"),
			new Query("See you", "ð"),
			new Query("I slept for four hours in total in dual rn", "Rm"),
			new Query("I always think and say im the alien in dualers", "Lol\nAlien dou ho wud gay !!"),
			new Query("jesrica bf想join-.-", "Any discount?"),
			new Query("I interview with our comp ta before too", "Lol"),
			new Query("Return a line ar maaaa", "?u mean paasword ?  !Ylc899769"),
			new Query("Maybe next round", "nobody i know on my list too"),
			new Query("十三", "Ok"),
			new Query("No ar no break", "Ohhh nvm"),
			new Query("Hmmm..", "But u need to get change"),
			new Query("Phone in not yet schedule ar", "Available choices only between 3rd to 11th"),
			new Query("咩好大鑊？？", "唔大件事咩ð"),
			new Query("可以呀", "其實想早啲"),
			new Query("Dklm-.-", "Sorry:P"),
			new Query("咁都要比佢贏既", "比下面都要啦"),
			new Query("12?", "U think eat what:D!"),
			new Query("And had to visit fcxking museums", "Kill me mei"),
			new Query("Can u book the table actually??", "Im meeting fyp professorrr"),
			new Query("Wow\nU will get in next round! Lol", "Hope so la"),
			new Query("Thz~~ u hv fun playing la", "Holiday ð"),
			new Query("Who don't late?", "Coz im a good girl"),
			new Query("Thank you!!", "ð"),
			new Query("Lot", "Lor"),
			new Query("炒埋一碟", "考到幾時?"),
			new Query("My mum and dad go out dinner tonight", "And my sis abandon me-.-"),
			new Query("Lol", "Why?"),
			new Query("Both come:D", "幾時到？"),
			new Query("Juz finish exam then dinner", "o"),
			new Query("Lol\nStudy bio", "Lol"),
			new Query("宜家去緊見工其實ð", "佢都唔得閒理我\n應該遲覆都冇所謂"),
			new Query("wanna go gar", "but headache"),
			new Query("佢真係好絕", "I dnt think so now"),
			new Query("Tony is ultimate clever", "Hahaha"),
			new Query("I can see stuff of winter term onlyð", "go to time table view of ur class schedule"),
			new Query(":)))", "Please change ur pic here as well"),
			new Query("Let it be", "Fix this thing jau suen la"),
			new Query("She dislikes með", "It's very impossible for u to have the grade lower than Carmen...."),
			new Query("still headache ð", ":((((("),
			new Query("操控本港同業拆息好似", "I should hv go for 公務員ð"),
			new Query("Hahaha", "Haha i dun really like celebrating my bday"),
			new Query("I was too hea", "Hahahahha"),
			new Query("Or the week after?", "Mon wed fri free :P"),
			new Query("ð", "Har"),
			new Query("我請", "Why so happy?"),
			new Query("Sure sure sure XD", "Next week?"),
			new Query("Im going to north pt la", "No maxim"),
			new Query("Lau farn bay nei", "Marn marn investigate"),
			new Query("1230 ma", "Yes"),
			new Query("Omg i meant..size? Depth?", "Flan or like pie dish?"),
			new Query("Then i ask ask", "I will open event tonight:) help ask ppl in hall thx tsuntsun:)"),
			new Query("I can only do what i can do so being freak out wont help ll", "Yaaaa"),
			new Query("Lol so random", "Good"),
			new Query("Sai pi shing", "Haha im already thinking of near places la"),
			new Query("Hahahha", "lol"),
			new Query("huh? Gum jei Hei dim Ar?", "Oo"),
			new Query("U ask ask Vincci la then, lol", "Lol\nIc"),
			new Query("yes ar...", "thats y he walked reli fast i cant catch up . .."),
			new Query("Not reli", "Timothy got in classes"),
			new Query("55", "食牛記ð"),
			new Query("coz i forogt,", "that was my project deadline n i worked overnight till 7pm the other day..."),
			new Query("U ask what i want to ask tim", "Haha"),
			new Query("I sleep in", "At home-.-"),
			new Query("Then u hv to gp with me, lol", "I wanna get in ar mgmt... Aiiii"),
			new Query("Lol\nSo normal to nervous la", "Who dun nervous..."),
			new Query("Than tell urself u are going to meet this mysterious girl in ur dream", "Sleep la dnt think too much..call u tmr when i wake up..still hv no idea where ur home is lol"),
			new Query("Lol\nGood luck la! U can get pass la!", "Idkkk"),
			new Query("Nei mei come lor", "Ngo nth to do"),
			new Query("Should be. But i will hv to confirm you tonight", "Ppp"),
			new Query("i ll let u know tmr", "okayy"),
			new Query("Poor you", "U said u wanna go right?"),
			new Query("I eat dried fruit everyday auntie", "my smoothies normally contain.....banana..apple...kale..spinach...blueberries...etcetc"),
			new Query("Cure now?", "haha hope its not as hard as reaching stars"),
			new Query("Just dun overlap wif my tutoring in Physics n Maths, cuz she always ask everything to one tutor like she asks me Excel functions for no reason... N my advice is to slow her down a bit... ð.. she loves asking all questions in a singlr class... that makes teaching a bit hard.", "U could whatsapp her then ð"),
			new Query("Im contributing to his grade ga", "Last time i slightly planned for his DSE..he seemed freak out a bit by my rare enthusiasm..lol"),
			new Query("Middle aged korean", "Thx to sung kim i can understand his korean accent"),
			new Query("XP", "no."),
			new Query("Did u?", "Not yet ><"),
			new Query("就係問你知唔知", "多女多事非"),
			new Query("Hahaha", "I might drop mgmt if u cant get in"),
			new Query("I have sent the email and waiting for reply", "OK"),
			new Query("Hey. The student asks again abt ICT tutoring as her mock comes up soon. I think she needs one or two classes before mock for her marked questions.", "Will u be too busy to handle these days? Im not sure if she will continue after she asks all her questions."),
			new Query("Chor 4", "Ic! Ok"),
			new Query("FYI\nI just got the mtr apt test", "Har now?!"),
			new Query("I hv a late class", "You prefer night ?"),
			new Query("I will play play la", "First choice it and second choice mt"),
			new Query("ð", "I like this icon very muchð"),
			new Query("Wtf", "lol"),
			new Query("Jong fai?", "Lol"),
			new Query("chi sib", "sin"),
			new Query("lol\nI just finished read once", "lol"),
			new Query("Becoz Ivan is lazy, lol", "hai wor"),
			new Query("認真姐..大家都攰想快啲完架下嘛", "omg i turned to a witch:("),
			new Query("係囉", "住九龍我真係會返"),
			new Query("Thz :)", "ð"),
			new Query("haha", "mine almost done la finally...but no model at all"),
			new Query("西鐵超快lol", "im home luuuu"),
			new Query("lol\njust once\ntonight agm again", "oh i forgot again"),
			new Query("sore throat", "went through a three-hour presentation class and have one of the presentations in slight fever then hotpot ytd now throat pain like hell -."),
			new Query("ð", "因為太珍貴次次都會忍住食晒啲渣ð"),
			new Query("Can you find the way?", "唔係好熟路"),
			new Query("i kept on stepping on the one in front-.-", "arrived."),
			new Query("返黎食？", "Yes"),
			new Query("i think what i did bad was that i didnt ask him good question", "i said i hv no question-__-"),
			new Query("ooooo\nthen ask her give up la", "lol"),
			new Query("Ho afraid", "kei sud i dont wana go"),
			new Query("唔知你趕唔趕scan transcript arr", "有人幫我"),
			new Query("有影印", "但我果部仲印到冇理由丟"),
			new Query("y I dun wake up earlier", "diu"),
			new Query(".....", "mgmt jau reli broken la"),
			new Query("i wont be free after 2pm", "我要下就五點幾後先得"),
			new Query(":D", "I am late, lol"),
			new Query("我會行過dem一兩次，唔打算長留", "我唔知返唔返好-.-不如一齊dem六點丫？"),
			new Query("I experience this before ma", "lol"),
			new Query("See doctor no need to payð", "Very stupid"),
			new Query("應該去non neway玩下", "(唔識打個名lol)"),
			new Query("not yet..still waiting", "i hope so arrr"),
			new Query("Sing Mingð", "我中左伏"),
			new Query("照補？", "sorry補唔到..勁痛"),
			new Query("My frd go for 10 days lor", "icic"),
			new Query("lol\nthanks!'", "hope really good luck la, lol"),
			new Query("ok thx", "buy sth sweet plz :D"),
			new Query("ho la ho la", "fail"),
			new Query("Hahahahahahha", "U sure ng sure ga"),
			new Query("but Stella don't hv another offer ma", "in yuen final round"),
			new Query("will u be in ust tonite? we better have a meeting on it la", "im at home ar"),
			new Query("語氣囉係", "認真姐..大家都攰想快啲完架下嘛"),
			new Query("aiii", "kaka..aii aii ha jau ai duc ju ga la！"),
			new Query("仲要調補習", "咁唔好返啦"),
			new Query("lol", "繼續走一走"),
			new Query("Apply sing con?", "think think sinð"),
			new Query("Only 4 chairpersons", "ok"),
			new Query("Think he'll call you when you're in.", "You'll be here around 12?"),
			new Query("yoyo", "有無source 呀個lab"),
			new Query("你邊個時間得?", "你邊個時間得?"),
			new Query("Btw, can you help me to pick up the industrial training reports?", "Btw, can you help me to pick up the industrial training reports?"),
			new Query("oh yeah", "okok"),
			new Query("haha", "go find a parttime now"),
			new Query("ð", "poor peter :P"),
			new Query("deadline set for sth i've juz  finished in half hour", "on 24/3"),
			new Query("定係我要腥返俾你先", "我想係手機試睇"),
			new Query("Me and bb mo", "yes"),
			new Query("y", "haha u afraid frogs too?"),
			new Query("terry is eating", "dual rm??"),
			new Query("hahaha", "lol"),
			new Query("under the ower of social medua", "media"),
			new Query("we didnt contact him afterward", "lol"),
			new Query("challenge him lor, lol", "thinking..as im below mean"),
			new Query("架巴士站站停", "架巴士站站停"),
			new Query("http://wenku.yingjiesheng.com/d13/bank/xingzhanbank.html", "thank you gen!:D"),
			new Query("btw", "Emily said accenture hr told her they pick 2-3ppl out of 8"),
			new Query("lol", "any mid terms left ar?"),
			new Query("u are the kai figure lol", "ð"),
			new Query("我係咪漏左部pi係枱面?", "yes lol"),
			new Query("u rrli need a better typo correction lop", "is ur super hard effort pay back? haha"),
			new Query("we r nt solving any problems", "but finding a point to make our work meaningful-.-"),
			new Query("u still have sauce from bufflo wow", "its not expired dnt worry lol"),
			new Query("ð", "ð"),
			new Query("Network security係 individual", "Unix 唔知"),
			new Query("南閘網球場落？", "maybe the best"),
			new Query("haha very hea", "but he asked me wt job hv i applied and which to take?"),
			new Query("不過未驚過lol", "dead ..im too old"),
			new Query("arrived=]", "gum fai"),
			new Query("lol\nchi sin! don't know y exam dou can't wake up", "actually no minibus mei?"),
			new Query("I am sorry", "I should have held back. I am sorry"),
			new Query("I thought you have good news from accenture", "not yet..still waiting"),
			new Query("haha", "and good luck to both of us in job hunting!! ush!!!"),
			new Query("too late", "lol"),
			new Query("??", "We r at hang hau now"),
			new Query("where to hand in ?", "just 9write"),
			new Query("gum fai", "im at gum chung"),
			new Query("ho take minibus dou 10 mins sin arrive the station lor", "much faster wor"),
			new Query("use 單數雙數", "?"),
			new Query("sorry occupied for the whole sunday", "only at 9 in the morning :/"),
			new Query("didnt pick up", "will find her again"),
			new Query("and went belated lol", "anyway..happy 23 XD"),
			new Query("Very stupid", "Max two sick leave per month"),
			new Query("eat laaaa", "r u having class with gen?"),
			new Query("definitely la", "ð"),
			new Query("我都返到了終於", "finally mtr or bus?"),
			new Query("Sunday ?", "Your Monday i thinj"),
			new Query("nice", "i got too many peaceful days so hv to work now lok"),
			new Query("yiiiii", "jung lum ju ask u help me buy sth to eat tim"),
			new Query("i rmb urs n terrys", "sick jor two weeks laaa..almost recover then become worse again"),
			new Query("ya u can read read sin ge so i can focus on the prorgrammig part", "ok"),
			new Query("i saw it opened on wed", "Ok thats  gd!"),
			new Query("im at gum chung", "where is it actually?"),
			new Query("who", "u and who?"),
			new Query("Lol. Okok", "maybe we can finish it quickly"),
			new Query("Ur ex back", "oh damn"),
			new Query("ar stupid jor haha", "data collection le...what did we do in collecting data apart from searching online? or fake sth again:P?"),
			new Query("are you uncomfortable now?", "goodnight"),
			new Query("係呀，你4月5號得閒嘛", "Ok啊，做咩？"),
			new Query("shit", "no"),
			new Query("哈好好玩", "我試過好似俾人ku住條頸囉這"),
			new Query("yes luckily", "hark say ngo"),
			new Query("唔計住", "哈"),
			new Query("lol\ndidnt mention u", "I search jor for the info on wiki"),
			new Query("i didnt drink much", "not even half bottle lol"),
			new Query("ð", "what time?"),
			new Query("im not so toxic la", "i wanna sleep too"),
			new Query("deadline set for sth i've juz  finished in half hour", "on 24/3"),
			new Query("beside moving the voice backwards..try to use throat more for lower notes", "and maybe add some pauses and take care at 尾音 of every words can help"),
			new Query("dinner so waken up my mum ja", "by*"),
			new Query("ðð", "but where do u live?"),
			new Query("dumbest", "ha"),
			new Query("good", "雖然唔係好識佢ð"),
			new Query("haha", "we all fail on that"),
			new Query("咁呀…", "你依家得閒？"),
			new Query("我係屋企都係用laptop", "我係屋企都係用laptop"),
			new Query("ha", "do u think he can e challenged?"),
			new Query("ok..so im dumbed", "i guess u got it:)"),
			new Query("its 1245", "maybe we meet afterwrds..ard 130?"),
			new Query("唔洗上堂咩", "only have class"),
			new Query("me dayoff tmr", "but will come bavk for revision"),
			new Query("ya", "i thought u said manning lol"),
			new Query("then forget ur laptop aha", "i want to-./"),
			new Query("damn heavy rain...", "not yet back ust?"),
			new Query("做咩呀？", "??"),
			new Query("Unix d notes 係個網係咪特登整錯架：p", "係囉…佢份份都錯既…"),
			new Query("ok", "thx:)"),
			new Query("no ar just fd told me la", "ur friend ate itð±ð"),
			new Query("i dnt hv this no. wor...", "lol i saw it in our group?"),
			new Query("haha", "u can call me ar ma"),
			new Query("oh, i know la", "so im not sure lot"),
			new Query("st", "phys again lol"),
			new Query("ð", "i think i should resume my 930 class after listening to ur encouraging story of going to 1030 classð"),
			new Query("okok", "need to book ar"),
			new Query("may be we can find some video clips...ð", "no la im always a kai figure"),
			new Query("both", "i learnt how to serve as well in cookery ð"),
			new Query("har?", "u mean 13 = 12?"),
			new Query("guess dou one of them is u", "lol"),
			new Query("事", "未必係一件好"),
			new Query("I am not intended to ask you about sung Kim in comp4901a group", "I am not intended to ask you about sung Kim in comp4901a group"),
			new Query("hmmm tmr should b free from 12-3", "i m typing cheat sheet at home"),
			new Query("ð", "唓"),
			new Query("-.-", "my sister wont lor"),
			new Query("i will feel so bad for any sickness", "im at 石崗ð"),
			new Query("我試過好似俾人ku住條頸囉這", "好玩ð")
			);

	private static void initialize() {
		final InstanceLoader instanceLoader = InstanceLoader.singleton();
		instanceLoader.setBinding(CachingHistoryDbAdapter.class, SqliteDbAdapter.class);
		instanceLoader.setBinding(HistoryDatabaseAdapter.class, ChunkdedHistoryDbAdapter.class);
	}

	private static Map<FeatureKey<?>, Object> extractFeatures(final HistoryEntry entry) {
		final Map<FeatureKey<?>, Object> tfIdfs = Maps.newHashMap();

		final FPStore fpStore = FPStore.singleton();
		final Set<Set<AtomicFeature<?>>> frequetPatterns = fpStore.getFrequetPatterns();

		final AtomicFeaturesManager atomicFeaturesManager = AtomicFeaturesManager.singleton();
		final List<AtomicFeature<?>> features = atomicFeaturesManager.getFeatures(entry);
		final HashSet<AtomicFeature<?>> featureSet = Sets.newHashSet(features);
		final List<Set<AtomicFeature<?>>> compoundFeatures = Lists.newLinkedList();
		final Multiset<Set<AtomicFeature<?>>> tfs = HashMultiset.create();

		for (final Set<AtomicFeature<?>> fp : frequetPatterns) {
			final boolean containsFp = featureSet.containsAll(fp);
			if (containsFp) {
				compoundFeatures.add(fp);
				tfs.add(fp);
			}
		}

		double maxCount = 0;
		final Set<Entry<Set<AtomicFeature<?>>>> entrySet = tfs.entrySet();
		for (final Entry<Set<AtomicFeature<?>>> counts : entrySet) {
			final int count = counts.getCount();
			if (count > maxCount) {
				maxCount = count;
			}
		}

		final TfIdfStore tfIdfStore = TfIdfStore.singleton();
		for (final Set<AtomicFeature<?>> feature : compoundFeatures) {
			final double idf = tfIdfStore.getInvertedDocumentFrequency(feature);
			final int count = tfs.count(feature);
			final double tf = count / maxCount;
			final double tfIdf = tf * idf;
			final TfIdfKey tfIdfKey = TfIdfKey.getKey(feature);
			tfIdfs.put(tfIdfKey, tfIdf);
		}

		return Collections.unmodifiableMap(tfIdfs);
	}

	private static void writeToDb(final String filename, final String[] tests, final Map<Query, List<Prediction>> predictions) {
		try {
			Class.forName("org.sqlite.JDBC");
		} catch (final ClassNotFoundException e1) {
			e1.printStackTrace();
			return;
		}
		final String connStr = "jdbc:sqlite:" + filename;

		// Making Connection
		Connection connection = null;
		try {
			connection = DriverManager.getConnection(connStr);
			connection.setAutoCommit(false);

			for (String testName : tests) {
				PreparedStatement statement = connection.prepareStatement(INSERT_TEST_SQL);
				statement.setString(1, testName);
				statement.execute();
			}

			final Set<Map.Entry<Query, List<Prediction>>> entrySet = predictions.entrySet();
			for (final Map.Entry<Query, List<Prediction>> entry : entrySet) {
				final Query key = entry.getKey();
				final String query = key.getQuery();
				final String actualResponse = key.getActualResponse();

				PreparedStatement statement = connection.prepareStatement(INSERT_QUERY_SQL);
				statement.setString(1, query);
				statement.setString(2, actualResponse);
				statement.execute();

				final int queryId = getId(connection);
				final List<Prediction> value = entry.getValue();
				for (final Prediction prediction : value) {
					final int testId = prediction.getTestId();
					final int charGiven = prediction.getCharGiven();
					final int rank = prediction.getRank();
					final String str = prediction.getPrediction();
					statement = connection.prepareStatement(INSERT_PREDICTION_SQL);
					statement.setInt(1, queryId);
					statement.setInt(2, testId);
					statement.setInt(3, charGiven);
					statement.setInt(4, rank);
					statement.setString(5, str);
					statement.execute();
				}
			}

			connection.commit();
		} catch (final SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				if (connection != null) {
					connection.close();
				}
			} catch (final SQLException e) {
				e.printStackTrace();
			}
		}
	}

	private static int getId(final Connection connection) throws SQLException {
		final Statement stmt = connection.createStatement();
		final ResultSet rs = stmt.executeQuery(ID_SQL);
		rs.next();
		final int id = rs.getInt(1);
		return id;
	}

	public static void main(final String[] args) throws IOException {
		initialize();

		final ChunkdedHistoryDbAdapter dbAdapter = (ChunkdedHistoryDbAdapter) HistoryDatabaseAdapter.singleton();
		final CachingHistoryDbAdapter underlyingDbAdapter = dbAdapter.getUnderlying();
		final FPStore fpStore = FPStore.singleton();
		final TfIdfStore tfIdfStore = TfIdfStore.singleton();
		final TfIdfFeatureStore tfIdfFeatureStore = TfIdfFeatureStore.singleton();
		final PredictionEngine predictionEngine = PredictionEngine.singleton();

		boolean initialized = true;

		String[] tests = new String[] {"With context", "Without context"};
		final Map<Query, List<Prediction>> map = new HashMap<Query, List<Prediction>>();
		for (int testId=1; testId<=tests.length; testId++) {
			if(testId != 1) {
				AtomicFeaturesManager.singleton().setProviders(Collections.<AtomicFeaturesProvider>singletonList(new TokenFeaturesProvider()));
				dbAdapter.setChunk(0);
			}

			for (int round = 1; round <= 10; round++) {
				if (!initialized) {
					dbAdapter.refresh();
					fpStore.refresh();
					tfIdfStore.refresh();
					tfIdfFeatureStore.refresh();
				}
				else {
					initialized = false;
				}

				System.out.println("Validating (" + round + "/" + N_FOLD + ")");

				final SortedSet<Integer> testSetIds = dbAdapter.getTestSetIds();
				for (final Integer id : testSetIds) {
					final NextHistoryInfo nextHistoryInfo = underlyingDbAdapter.nextHistoryEntry(id);
					final HistoryEntry nextHistoryEntry = nextHistoryInfo == null ? null : (nextHistoryInfo.interval < MAX_DEAD_AIR_INTERVAL ? nextHistoryInfo.history : null);
					if (nextHistoryEntry != null) {
						final HistoryEntry history = underlyingDbAdapter.getHistoryById(id);
						final String message = history.getMessage();
						final String target = nextHistoryEntry.getMessage();

						final Query query = new Query(message, target);

						if(QUERYS.contains(query)) {


							final String sender = history.getSender();
							final String nextSender = nextHistoryEntry.getSender();
							final MessageType type = sender.equals(nextSender) ? MessageType.SUPPLEMENT : MessageType.REPLY;

							final Map<FeatureKey<?>, Object> features = extractFeatures(history);

							List<Prediction> pList = map.get(query);
							if(pList == null) {
								pList = Lists.newLinkedList();
								map.put(query, pList);
							}

							final StringBuilder incomplete = new StringBuilder();
							final String[] splites = target.split("");
							for (final String character : splites) {
								incomplete.append(character);
								final String incompleteStr = incomplete.toString();
								final List<String> predictions = predictionEngine.getPredictions(features, incompleteStr, type);
								if (predictions.isEmpty()) {
									break;
								}

								final int length = incompleteStr.length();
								int rank = 1;
								for (final String pred : predictions) {
									pList.add(new Prediction(testId, length, rank, pred));
									rank++;
								}
							}
						}
					}
				}

				dbAdapter.nextChunk();
			}
		}
		writeToDb("db.sqlite", tests, map);
	}
}
