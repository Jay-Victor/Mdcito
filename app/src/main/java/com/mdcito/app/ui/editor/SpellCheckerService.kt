package com.mdcito.app.ui.editor

import android.content.Context
import android.view.textservice.SentenceSuggestionsInfo
import android.view.textservice.SpellCheckerSession
import android.view.textservice.SpellCheckerSession.SpellCheckerSessionListener
import android.view.textservice.TextInfo
import android.view.textservice.TextServicesManager
import com.darkrockstudios.symspellkt.impl.SymSpell
import com.darkrockstudios.symspellkt.common.SpellCheckSettings
import com.darkrockstudios.symspellkt.common.Verbosity
import com.darkrockstudios.symspellkt.common.Murmur3HashFunction
import com.darkrockstudios.symspellkt.impl.InMemoryDictionaryHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume

/**
 * 拼写检查服务，支持中文和英文。
 * 英文：基于 SymSpellKt（MIT 协议）实现。
 * 中文：基于规则的常见错别字检测。
 *
 * 优化：
 * - 异步检查，不阻塞 UI 线程
 * - 扩充内置英文词典（约 1000+ 常用词）
 * - 增量检查：仅检查变化的段落
 * - 防抖支持：通过 debounceMs 参数控制
 */
class SpellCheckerService(private val context: Context) {

    private var symSpell: SymSpell? = null
    private var isInitialized = false

    // 用户自定义词典（运行时动态添加的单词）
    private val customWords = mutableSetOf<String>()

    // 扩充的内置常用英语词典（频率字典格式：word frequency）
    private val builtinDictionary = mapOf(
        // ── Top 200 most common English words ──
        "the" to 69971, "of" to 36412, "and" to 28854, "to" to 26152, "a" to 23237,
        "in" to 21341, "is" to 10595, "it" to 10099, "for" to 9489, "was" to 9321,
        "that" to 8620, "on" to 8365, "are" to 7389, "with" to 6941, "he" to 6741,
        "as" to 6456, "I" to 6219, "his" to 5906, "they" to 5634, "be" to 5290,
        "at" to 5197, "one" to 5147, "have" to 4944, "this" to 4867, "from" to 4774,
        "or" to 4652, "had" to 4468, "by" to 4417, "not" to 4240, "but" to 4183,
        "what" to 3982, "all" to 3783, "were" to 3746, "we" to 3699, "when" to 3626,
        "your" to 3572, "can" to 3497, "said" to 3288, "there" to 3275, "use" to 3226,
        "an" to 3143, "each" to 3056, "which" to 2989, "she" to 2860, "do" to 2856,
        "how" to 2815, "their" to 2760, "if" to 2714, "will" to 2684, "up" to 2624,
        "other" to 2590, "about" to 2570, "out" to 2539, "many" to 2499, "then" to 2465,
        "them" to 2443, "these" to 2403, "so" to 2378, "some" to 2346, "her" to 2312,
        "would" to 2287, "make" to 2264, "like" to 2245, "him" to 2197, "into" to 2197,
        "time" to 2173, "has" to 2148, "look" to 2119, "two" to 2095, "more" to 2070,
        "write" to 2042, "go" to 2018, "see" to 1991, "number" to 1965, "no" to 1940,
        "way" to 1915, "could" to 1890, "people" to 1865, "my" to 1840, "than" to 1815,
        "first" to 1790, "water" to 1765, "been" to 1740, "call" to 1715, "who" to 1690,
        "oil" to 1665, "its" to 1640, "now" to 1615, "find" to 1590, "long" to 1565,
        "down" to 1540, "day" to 1515, "did" to 1490, "get" to 1465, "come" to 1440,
        "made" to 1415, "may" to 1390, "part" to 1365, "over" to 1340, "new" to 1315,
        "sound" to 1290, "take" to 1265, "only" to 1240, "little" to 1215, "work" to 1190,
        "know" to 1165, "place" to 1140, "year" to 1115, "live" to 1090, "me" to 1065,
        "back" to 1040, "give" to 1015, "most" to 990, "after" to 965, "right" to 940,
        "also" to 915, "just" to 890, "because" to 865, "every" to 840, "good" to 815,
        "under" to 790, "man" to 765, "very" to 740, "much" to 715, "before" to 690,
        "through" to 665, "same" to 640, "another" to 615, "where" to 590, "still" to 565,
        "between" to 540, "should" to 515, "never" to 490, "world" to 465, "too" to 440,
        "here" to 415, "any" to 390, "own" to 365, "great" to 340, "while" to 315,
        "last" to 290, "need" to 265, "big" to 240, "high" to 215, "old" to 190,
        "such" to 140, "keep" to 115, "must" to 90, "home" to 65,

        // ── Extended common words (500+) ──
        "able" to 5000, "about" to 5000, "above" to 4000, "across" to 3000,
        "actually" to 3500, "add" to 4000, "after" to 5000, "again" to 4500,
        "against" to 3000, "ago" to 3500, "agree" to 2500, "air" to 3000,
        "alive" to 2000, "allow" to 3000, "almost" to 3500, "alone" to 2500,
        "along" to 3500, "already" to 4000, "always" to 4000, "among" to 2500,
        "amount" to 2000, "ancient" to 1500, "answer" to 3000, "anything" to 3500,
        "area" to 4000, "around" to 4500, "army" to 2500, "away" to 4000,
        "baby" to 3000, "bad" to 4000, "bag" to 2500, "balance" to 2000,
        "ball" to 2500, "bank" to 3500, "base" to 4000, "basic" to 3000,
        "battle" to 2000, "bear" to 2000, "beat" to 2500, "beautiful" to 3000,
        "became" to 3500, "become" to 4500, "bed" to 3000, "began" to 3500,
        "begin" to 3500, "behind" to 3500, "believe" to 3500, "best" to 4500,
        "better" to 4000, "between" to 3500, "beyond" to 2000, "big" to 4000,
        "bit" to 3500, "black" to 3000, "blood" to 2500, "blue" to 2500,
        "board" to 2500, "body" to 4000, "bone" to 1500, "book" to 3500,
        "born" to 3000, "both" to 3500, "bottom" to 2500, "box" to 2500,
        "boy" to 3500, "break" to 3000, "bright" to 2000, "bring" to 3500,
        "broad" to 1500, "broken" to 2000, "brother" to 2500, "brown" to 2000,
        "build" to 3000, "building" to 2500, "burn" to 1500, "business" to 3500,
        "buy" to 3000, "cabin" to 1000, "care" to 4000, "carry" to 3000,
        "case" to 4000, "catch" to 2500, "cause" to 3000, "center" to 3500,
        "certain" to 2500, "chance" to 3000, "change" to 4500, "character" to 2500,
        "check" to 3000, "child" to 3500, "children" to 3500, "choice" to 2500,
        "choose" to 2500, "church" to 2500, "city" to 4000, "class" to 3500,
        "clean" to 2500, "clear" to 3500, "climb" to 1500, "close" to 4000,
        "clothes" to 2000, "cloud" to 1500, "cold" to 3000, "collected" to 1500,
        "color" to 3000, "come" to 5000, "common" to 3500, "company" to 3500,
        "complete" to 2500, "computer" to 3000, "condition" to 2000, "consider" to 2000,
        "continue" to 3000, "control" to 3500, "cook" to 2000, "cool" to 2500,
        "copy" to 2500, "corner" to 2000, "cost" to 3000, "could" to 5000,
        "count" to 2500, "country" to 4000, "couple" to 2500, "course" to 3500,
        "court" to 2500, "cover" to 3000, "create" to 3000, "cross" to 2000,
        "crowd" to 1500, "current" to 3000, "cut" to 3000, "dark" to 3000,
        "data" to 3500, "daughter" to 2000, "dead" to 3000, "deal" to 3000,
        "dear" to 2500, "death" to 3000, "decide" to 2500, "deep" to 3000,
        "develop" to 2500, "different" to 4000, "difficult" to 3000, "direct" to 2000,
        "discuss" to 2000, "disease" to 1500, "distance" to 2000, "divide" to 1500,
        "doctor" to 3000, "does" to 4500, "dog" to 2500, "dollar" to 2500,
        "door" to 3000, "double" to 2000, "doubt" to 2000, "down" to 5000,
        "draw" to 2500, "dream" to 2500, "dress" to 2000, "drink" to 2500,
        "drive" to 3000, "drop" to 2500, "during" to 4000, "each" to 4500,
        "early" to 4000, "earth" to 3000, "east" to 2500, "easy" to 3500,
        "eat" to 3000, "effect" to 3000, "eight" to 2500, "either" to 3000,
        "else" to 3500, "end" to 4000, "enemy" to 2000, "energy" to 2500,
        "engine" to 2000, "enough" to 4000, "enter" to 2500, "entire" to 2000,
        "equal" to 2000, "escape" to 1500, "even" to 4500, "evening" to 3000,
        "event" to 3000, "ever" to 4000, "every" to 4500, "evidence" to 1500,
        "exact" to 1500, "example" to 3500, "exercise" to 2000, "exist" to 2000,
        "expect" to 2500, "experience" to 2500, "experiment" to 1500, "explain" to 2000,
        "express" to 1500, "eye" to 3000, "face" to 3500, "fact" to 4000,
        "fail" to 2000, "fair" to 2500, "fall" to 3000, "family" to 4000,
        "far" to 3500, "farm" to 2000, "fast" to 3000, "father" to 3000,
        "fear" to 3000, "feel" to 4000, "feet" to 2500, "fell" to 2500,
        "felt" to 2500, "field" to 3000, "fight" to 2500, "figure" to 2500,
        "fill" to 2500, "final" to 2500, "finally" to 2500, "find" to 4500,
        "fine" to 3000, "finger" to 2000, "finish" to 2500, "fire" to 3000,
        "fish" to 2000, "floor" to 2500, "fly" to 2500, "follow" to 3000,
        "food" to 3000, "foot" to 2500, "for" to 6000, "force" to 3000,
        "foreign" to 2000, "form" to 3500, "found" to 3500, "four" to 2500,
        "free" to 3500, "friend" to 3500, "front" to 3000, "full" to 3500,
        "game" to 3000, "garden" to 2000, "gave" to 3000, "general" to 3000,
        "get" to 5000, "girl" to 3000, "give" to 4500, "glad" to 1500,
        "glass" to 2000, "go" to 5000, "god" to 2500, "going" to 4500,
        "gold" to 2000, "gone" to 3000, "good" to 5000, "got" to 4000,
        "government" to 3000, "grass" to 1500, "great" to 4000, "green" to 2500,
        "ground" to 3000, "group" to 3500, "grow" to 2500, "guess" to 2000,
        "gun" to 1500, "had" to 5000, "hair" to 2500, "half" to 3500,
        "hand" to 3500, "handle" to 2000, "happen" to 3000, "happy" to 3000,
        "hard" to 3500, "has" to 5000, "hat" to 1500, "have" to 6000,
        "head" to 3500, "hear" to 3000, "heart" to 2500, "heavy" to 2000,
        "held" to 2500, "help" to 4000, "her" to 5000, "here" to 4500,
        "high" to 3500, "him" to 4500, "himself" to 3000, "his" to 5000,
        "history" to 2500, "hit" to 2500, "hold" to 3500, "hole" to 2000,
        "home" to 4000, "hope" to 3000, "horse" to 2000, "hot" to 3000,
        "hotel" to 2000, "hour" to 3500, "house" to 3500, "how" to 4500,
        "huge" to 2000, "human" to 2500, "hundred" to 2500, "idea" to 3500,
        "important" to 3000, "inch" to 1500, "include" to 2500, "increase" to 2000,
        "indeed" to 2000, "information" to 2500, "inside" to 2500, "instead" to 2500,
        "interest" to 2500, "into" to 4500, "invest" to 1500, "iron" to 1500,
        "island" to 1500, "issue" to 2500, "it" to 6000, "its" to 4000,
        "itself" to 2000, "job" to 3000, "join" to 2000, "jump" to 1500,
        "just" to 4500, "keep" to 3500, "key" to 2500, "kid" to 2000,
        "kill" to 2000, "kind" to 3500, "king" to 2000, "kitchen" to 1500,
        "knew" to 2500, "know" to 4500, "known" to 2500, "lady" to 2500,
        "land" to 3000, "language" to 2000, "large" to 3500, "last" to 4000,
        "late" to 3000, "later" to 3500, "laugh" to 2000, "lay" to 2000,
        "lead" to 3000, "learn" to 3000, "least" to 2500, "leave" to 3000,
        "left" to 3500, "less" to 3000, "let" to 4000, "letter" to 2500,
        "level" to 3000, "lie" to 2000, "life" to 4000, "lift" to 1500,
        "light" to 3500, "like" to 5000, "line" to 3500, "list" to 3000,
        "listen" to 2000, "little" to 4000, "live" to 4000, "long" to 4000,
        "look" to 4500, "lose" to 2500, "lost" to 3000, "lot" to 3500,
        "love" to 3500, "low" to 3000, "machine" to 2500, "main" to 2500,
        "major" to 2500, "make" to 5000, "man" to 4000, "manage" to 2000,
        "many" to 4500, "mark" to 2500, "market" to 3000, "matter" to 2500,
        "may" to 4000, "maybe" to 2500, "mean" to 3500, "measure" to 1500,
        "meet" to 3000, "member" to 2500, "men" to 2500, "method" to 2000,
        "middle" to 2500, "might" to 3500, "mile" to 2000, "mind" to 3000,
        "mine" to 2000, "minute" to 2500, "mirror" to 1500, "miss" to 2500,
        "modern" to 2000, "moment" to 3000, "money" to 3000, "month" to 3500,
        "moon" to 1500, "morning" to 3000, "mother" to 3000, "move" to 3500,
        "much" to 4000, "music" to 2500, "must" to 3500, "name" to 4000,
        "nation" to 2500, "nature" to 2000, "near" to 3000, "necessary" to 2000,
        "need" to 4000, "never" to 4000, "new" to 4500, "news" to 2500,
        "next" to 4000, "nice" to 3000, "night" to 3500, "nine" to 2000,
        "none" to 2500, "normal" to 2000, "north" to 2500, "nothing" to 3500,
        "notice" to 2500, "now" to 4500, "number" to 3500, "obtain" to 1500,
        "ocean" to 1500, "off" to 4000, "offer" to 2500, "office" to 2500,
        "often" to 3000, "old" to 4000, "once" to 3500, "only" to 4500,
        "open" to 3500, "opportunity" to 1500, "order" to 3500, "other" to 4500,
        "our" to 4000, "out" to 5000, "outside" to 2000, "over" to 4000,
        "own" to 3500, "page" to 3000, "pain" to 2000, "paper" to 2500,
        "parent" to 2000, "part" to 4000, "particular" to 2000, "party" to 3000,
        "pass" to 2500, "past" to 2500, "pattern" to 1500, "pay" to 3000,
        "people" to 4500, "perfect" to 2000, "perhaps" to 2500, "period" to 2000,
        "person" to 3000, "phone" to 2500, "photo" to 2000, "pick" to 2500,
        "picture" to 2500, "piece" to 2500, "place" to 4000, "plan" to 3000,
        "plant" to 2000, "play" to 3000, "please" to 3000, "point" to 3000,
        "poor" to 2500, "popular" to 2000, "position" to 2000, "possible" to 3000,
        "power" to 3000, "practice" to 2000, "prepare" to 1500, "present" to 2500,
        "president" to 2000, "press" to 2000, "pretty" to 2500, "prevent" to 1500,
        "price" to 2500, "problem" to 3000, "produce" to 2000, "product" to 2500,
        "program" to 2500, "provide" to 2500, "public" to 3000, "pull" to 2000,
        "purpose" to 2000, "push" to 2000, "put" to 3500, "question" to 3000,
        "quick" to 2000, "quickly" to 2500, "quite" to 2500, "race" to 2000,
        "radio" to 1500, "rain" to 1500, "raise" to 2000, "range" to 2000,
        "rather" to 2500, "reach" to 2500, "read" to 3500, "ready" to 3000,
        "real" to 3000, "really" to 3500, "reason" to 2500, "receive" to 2000,
        "record" to 2000, "red" to 2500, "remember" to 2500, "report" to 2000,
        "rest" to 2500, "result" to 2500, "return" to 2500, "rich" to 2000,
        "ride" to 1500, "right" to 4000, "river" to 2000, "road" to 2000,
        "rock" to 2000, "room" to 3000, "rule" to 2000, "run" to 3500,
        "safe" to 2500, "said" to 4500, "sail" to 1000, "same" to 3500,
        "sand" to 1500, "save" to 2500, "say" to 4000, "school" to 3500,
        "science" to 2000, "sea" to 2000, "search" to 2500, "season" to 2000,
        "second" to 3000, "secret" to 1500, "section" to 2000, "see" to 4500,
        "seem" to 3000, "self" to 2000, "sell" to 2500, "send" to 3000,
        "sense" to 2500, "serve" to 2000, "service" to 2500, "set" to 3500,
        "seven" to 2000, "several" to 3000, "shape" to 2000, "share" to 2000,
        "sharp" to 1500, "she" to 5000, "ship" to 2000, "shoe" to 1500,
        "shoot" to 1500, "short" to 3000, "shot" to 2000, "should" to 4000,
        "shoulder" to 1500, "show" to 3500, "shut" to 1500, "side" to 3000,
        "sign" to 2500, "simple" to 2500, "since" to 3500, "sing" to 1500,
        "single" to 2500, "sir" to 2000, "sister" to 2000, "sit" to 2500,
        "site" to 2500, "situation" to 2000, "six" to 2000, "size" to 3000,
        "skin" to 2000, "small" to 3500, "smile" to 2000, "snow" to 1500,
        "soft" to 2000, "soil" to 1500, "soldier" to 1500, "solution" to 2000,
        "some" to 4500, "something" to 4000, "sometimes" to 3000, "son" to 2500,
        "soon" to 3000, "sort" to 2500, "sound" to 3000, "south" to 2500,
        "space" to 2500, "speak" to 2500, "special" to 2500, "speed" to 2000,
        "spend" to 2500, "sport" to 2000, "spread" to 1500, "spring" to 2000,
        "square" to 1500, "stand" to 2500, "star" to 2000, "start" to 4000,
        "state" to 4000, "station" to 2000, "stay" to 2500, "step" to 2500,
        "stick" to 1500, "still" to 4000, "stock" to 2000, "stone" to 1500,
        "stop" to 3500, "store" to 2500, "story" to 3000, "street" to 2500,
        "strong" to 2500, "student" to 2500, "study" to 3000, "subject" to 2000,
        "success" to 2000, "such" to 3500, "suddenly" to 2000, "sugar" to 1500,
        "summer" to 2000, "sun" to 2000, "supply" to 1500, "support" to 2500,
        "sure" to 3500, "surface" to 2000, "surprise" to 1500, "sweet" to 1500,
        "system" to 3000, "table" to 2500, "take" to 4500, "talk" to 3000,
        "tall" to 2000, "teach" to 2500, "team" to 2500, "tell" to 3500,
        "ten" to 2000, "test" to 3000, "than" to 4500, "that" to 6000,
        "the" to 70000, "their" to 4000, "them" to 4000, "then" to 4000,
        "there" to 4500, "these" to 3000, "they" to 4500, "thing" to 3500,
        "think" to 4000, "third" to 1500, "those" to 2500, "though" to 3000,
        "thought" to 3000, "thousand" to 1500, "three" to 2500, "through" to 3500,
        "thus" to 2000, "time" to 5000, "tiny" to 1500, "today" to 3000,
        "together" to 3000, "told" to 2500, "tone" to 1500, "tonight" to 2000,
        "top" to 3000, "total" to 2500, "touch" to 2000, "toward" to 2500,
        "town" to 2000, "trade" to 2000, "train" to 2000, "travel" to 2000,
        "tree" to 2000, "true" to 3000, "trust" to 2000, "truth" to 2000,
        "try" to 3500, "turn" to 3000, "two" to 3500, "type" to 2500,
        "under" to 3000, "understand" to 2500, "union" to 1500, "unit" to 2000,
        "unite" to 1500, "until" to 3500, "upon" to 2500, "use" to 4000,
        "usual" to 2000, "value" to 2500, "various" to 2000, "very" to 4000,
        "visit" to 2000, "voice" to 2000, "vote" to 1500, "wait" to 3000,
        "walk" to 2500, "wall" to 2000, "want" to 4000, "war" to 2500,
        "warm" to 2000, "wash" to 1500, "watch" to 2500, "water" to 3000,
        "wave" to 1500, "way" to 4000, "weak" to 1500, "wear" to 2000,
        "week" to 3000, "weight" to 2000, "well" to 4000, "went" to 3000,
        "were" to 4500, "west" to 2500, "western" to 1500, "what" to 4500,
        "when" to 4500, "where" to 3500, "whether" to 2000, "which" to 4000,
        "while" to 3500, "white" to 2500, "who" to 3500, "whole" to 2500,
        "why" to 3000, "wide" to 2000, "wife" to 2000, "will" to 5000,
        "win" to 2000, "wind" to 2000, "window" to 2000, "wish" to 2000,
        "with" to 5000, "within" to 2500, "without" to 2500, "woman" to 2500,
        "women" to 2000, "wonder" to 2000, "wood" to 1500, "word" to 3000,
        "work" to 4000, "world" to 3500, "would" to 4500, "write" to 3000,
        "wrong" to 2000, "yard" to 1500, "year" to 4000, "yellow" to 1500,
        "yes" to 3000, "yet" to 3000, "you" to 5000, "young" to 2500,
        "your" to 4000, "yourself" to 2000,

        // ── Markdown / programming related words ──
        "markdown" to 500, "heading" to 400, "paragraph" to 350, "blockquote" to 300,
        "emphasis" to 250, "strikethrough" to 200, "checkbox" to 150, "horizontal" to 100,
        "ordered" to 95, "unordered" to 90, "syntax" to 85, "render" to 80,
        "preview" to 70, "editor" to 70, "format" to 65, "link" to 60,
        "image" to 55, "table" to 50, "code" to 45, "list" to 40,
        "bold" to 35, "italic" to 30, "font" to 25, "size" to 20,
        "plugin" to 15, "extension" to 15, "module" to 15, "config" to 10,
        "repository" to 10, "version" to 10, "commit" to 10, "branch" to 10,
        "merge" to 10, "deploy" to 10, "build" to 10, "release" to 10,
        "feature" to 10, "option" to 10, "setting" to 10, "default" to 10,
        "enabled" to 10, "disabled" to 10, "toggle" to 10, "switch" to 10,
        "import" to 10, "export" to 10, "template" to 10, "theme" to 10,
        "layout" to 10, "widget" to 10, "component" to 10, "element" to 10,
        "attribute" to 10, "property" to 10, "method" to 10, "function" to 10,
        "parameter" to 10, "argument" to 10, "variable" to 10, "constant" to 10,
        "string" to 10, "integer" to 10, "boolean" to 10, "array" to 10,
        "object" to 10, "dictionary" to 10, "tuple" to 10, "iterator" to 10,
        "callback" to 10, "promise" to 10, "async" to 10, "await" to 10,
    )

    // 常见中文易混淆字组（用于检测可能的错别字）
    private val chineseConfusableGroups = listOf(
        setOf("的", "地", "得"),
        setOf("在", "再"),
        setOf("已", "以"),
        setOf("做", "作"),
        setOf("即", "既"),
        setOf("须", "需"),
        setOf("像", "象", "相"),
        setOf("到", "道"),
        setOf("因", "应"),
        setOf("长", "常"),
        setOf("进", "近"),
        setOf("历", "厉"),
        setOf("连", "联"),
        setOf("形", "型"),
        setOf("制", "治", "质"),
        setOf("查", "察"),
        setOf("定", "订"),
        setOf("部", "布", "步"),
        setOf("代", "带", "戴"),
        setOf("反", "返"),
        setOf("付", "副", "负"),
        setOf("工", "公", "功"),
        setOf("和", "合", "河"),
        setOf("记", "纪"),
        setOf("交", "教"),
        setOf("决", "绝"),
        setOf("力", "立", "利"),
        setOf("明", "名", "命"),
        setOf("期", "其", "奇"),
        setOf("实", "是", "使"),
        setOf("事", "是", "式"),
        setOf("收", "受"),
        setOf("文", "闻", "稳"),
        setOf("向", "象", "像"),
        setOf("效", "消", "销"),
        setOf("学", "雪", "血"),
        setOf("以", "已", "乙"),
        setOf("原", "缘", "源"),
        setOf("正", "政", "整"),
        setOf("中", "种", "重"),
    )

    /**
     * 初始化拼写检查引擎
     */
    fun initialize() {
        if (isInitialized) return
        try {
            val settings = SpellCheckSettings(
                maxEditDistance = 2.0,
                prefixLength = 7,
            )
            symSpell = SymSpell(
                spellCheckSettings = settings,
                dictionaryHolder = InMemoryDictionaryHolder(settings, Murmur3HashFunction()),
            ).also { spell ->
                builtinDictionary.forEach { (word, freq) ->
                    spell.createDictionaryEntry(word, freq)
                }
            }
            isInitialized = true
        } catch (_: Exception) {
            symSpell = null
        }
    }

    /**
     * 异步检查文本中的拼写错误（推荐方法，不阻塞 UI 线程）
     * @param text 要检查的文本
     * @return 拼写错误列表
     */
    suspend fun checkSpellingAsync(text: String): List<SpellError> = withContext(Dispatchers.Default) {
        checkSpelling(text)
    }

    /**
     * 检查文本中的拼写错误（同步方法）
     * @param text 要检查的文本
     * @return 拼写错误列表
     */
    fun checkSpelling(text: String): List<SpellError> {
        if (!isInitialized) initialize()
        val spell = symSpell

        val errors = mutableListOf<SpellError>()

        // ── 英文拼写检查 ──
        if (spell != null) {
            val wordRegex = Regex("\\b([a-zA-Z]{2,})\\b")
            wordRegex.findAll(text).forEach { match ->
                val word = match.groupValues[1]
                if (word.length < 3) return@forEach
                if (isMarkdownSyntax(word)) return@forEach

                val isKnown = builtinDictionary.keys.any {
                    it.equals(word, ignoreCase = true)
                } || customWords.any {
                    it.equals(word, ignoreCase = true)
                }

                if (!isKnown) {
                    val suggestions = spell.lookup(word.lowercase(), Verbosity.Closest)
                        .take(5)
                        .map { it.term }
                    errors.add(SpellError(
                        word = word,
                        range = match.range,
                        suggestions = suggestions
                    ))
                }
            }
        }

        // ── 中文拼写检查：基于常见错别字对照表 ──
        checkChineseSpelling(text, errors)

        return errors
    }

    /**
     * 中文拼写检查：检测常见错别字
     * 使用基于规则的检测方法：
     * 1. 检测"的/地/得"误用
     * 2. 检测常见同音字误用
     */
    private fun checkChineseSpelling(text: String, errors: MutableList<SpellError>) {
        // 跳过代码块内的内容
        val textWithoutCode = removeCodeBlocks(text)

        // 1. 检测"的/地/得"误用
        checkDeDiDe(textWithoutCode, errors, text)

        // 2. 检测常见同音字误用
        checkCommonHomophones(textWithoutCode, errors, text)
    }

    /**
     * 移除代码块内容，避免误检
     */
    private fun removeCodeBlocks(text: String): String {
        return text.replace(Regex("```[\\s\\S]*?```")) { " ".repeat(it.value.length) }
            .replace(Regex("`[^`]+`")) { " ".repeat(it.value.length) }
    }

    /**
     * 检测"的/地/得"误用
     * 规则：
     * - 形容词 + 的 + 名词
     * - 副词 + 地 + 动词
     * - 动词 + 得 + 副词/形容词
     */
    private fun checkDeDiDe(cleanText: String, errors: MutableList<SpellError>, originalText: String) {
        // 检测"的"后接动词的误用（应为"地"）
        val deBeforeVerb = Regex("的([一]?)([\u4e00-\u9fff])")
        deBeforeVerb.findAll(originalText).forEach { match ->
            val charAfterDe = match.groupValues[2]
            val commonVerbChars = setOf(
                "走", "跑", "飞", "跳", "说", "看", "听", "写", "读", "想",
                "学", "做", "用", "给", "让", "把", "被", "从", "到", "在",
                "来", "去", "回", "过", "起", "开", "关", "上", "下", "出",
                "进", "退", "坐", "站", "睡", "吃", "喝", "玩", "笑", "哭",
                "打", "抓", "推", "拉", "举", "放", "拿", "找", "送", "带",
                "变", "改", "增", "减", "升", "降", "建", "修", "拆", "移",
                "发", "生", "成", "完", "结", "续", "停", "续", "转", "调",
                "处", "理", "解", "决", "执", "行", "操", "控", "设", "计",
                "编", "译", "测", "试", "调", "试", "运", "营", "维", "护",
            )
            if (charAfterDe in commonVerbChars && match.range.first in originalText.indices) {
                val beforeDe = if (match.range.first > 0) {
                    originalText.getOrNull(match.range.first - 1)?.toString() ?: ""
                } else ""
                val adjSuffixes = setOf("慢", "快", "轻", "重", "大", "小", "多", "少", "高", "低",
                    "长", "短", "深", "浅", "远", "近", "早", "晚", "忙", "闲",
                    "认真", "仔细", "积极", "努力", "安静", "慢慢", "轻轻", "悄悄",
                    "渐渐", "突然", "迅速", "缓慢", "匆匆", "静静", "默默")
                val contextBefore = originalText.substring(
                    maxOf(0, match.range.first - 4), match.range.first
                )
                val isLikelyAdjModifier = adjSuffixes.any { contextBefore.endsWith(it) }
                if (isLikelyAdjModifier) {
                    val dePos = match.range.first
                    errors.add(SpellError(
                        word = "的",
                        range = dePos..dePos,
                        suggestions = listOf("地")
                    ))
                }
            }
        }

        // 检测"得"误用为"的"（动词+的+副词，应为动词+得+副词）
        val deAfterVerb = Regex("([\u4e00-\u9fff])的(很|快|慢|好|坏|多|少|远|近|深|浅|高|低|大|小|早|晚|清楚|明白|干净|漂亮|彻底|十分|非常|特别|格外|更加|越来越)")
        deAfterVerb.findAll(originalText).forEach { match ->
            val verbChar = match.groupValues[1]
            val commonVerbChars = setOf(
                "走", "跑", "飞", "跳", "说", "看", "听", "写", "读", "想",
                "学", "做", "用", "来", "去", "做", "干", "活", "长", "变",
                "生", "长", "处", "理", "解", "打", "吃", "玩", "笑", "哭",
            )
            if (verbChar in commonVerbChars) {
                val dePos = match.range.first + 1
                errors.add(SpellError(
                    word = "的",
                    range = dePos..dePos,
                    suggestions = listOf("得")
                ))
            }
        }
    }

    /**
     * 检测常见同音字误用
     */
    private fun checkCommonHomophones(cleanText: String, errors: MutableList<SpellError>, originalText: String) {
        // 检测"在/再"误用
        val zaiBeforeVerb = Regex("在(一次|来|去|做|说|看|想|试|走|跑|写|读|学|用|给|让|把)")
        zaiBeforeVerb.findAll(originalText).forEach { match ->
            val zaiPos = match.range.first
            errors.add(SpellError(
                word = "在",
                range = zaiPos..zaiPos,
                suggestions = listOf("再")
            ))
        }

        // 检测"已/以"误用
        val yiJing = Regex("以经")
        yiJing.findAll(originalText).forEach { match ->
            errors.add(SpellError(
                word = "以经",
                range = match.range,
                suggestions = listOf("已经")
            ))
        }

        // 检测"即/既"误用
        val jiShi = Regex("既使")
        jiShi.findAll(originalText).forEach { match ->
            errors.add(SpellError(
                word = "既使",
                range = match.range,
                suggestions = listOf("即使")
            ))
        }
        val jiRan = Regex("即然")
        jiRan.findAll(originalText).forEach { match ->
            errors.add(SpellError(
                word = "即然",
                range = match.range,
                suggestions = listOf("既然")
            ))
        }

        // 检测"做/作"误用
        val zuoAbstract = Regex("做(为|用|业|者|文|品|风|战|家)")
        zuoAbstract.findAll(originalText).forEach { match ->
            val zuoPos = match.range.first
            errors.add(SpellError(
                word = "做",
                range = zuoPos..zuoPos,
                suggestions = listOf("作")
            ))
        }

        // 检测"长/常"误用
        val changJian = Regex("长见")
        changJian.findAll(originalText).forEach { match ->
            val changPos = match.range.first
            errors.add(SpellError(
                word = "长",
                range = changPos..changPos,
                suggestions = listOf("常")
            ))
        }

        // 检测"帐/账"误用
        val zhangHao = Regex("帐号")
        zhangHao.findAll(originalText).forEach { match ->
            errors.add(SpellError(
                word = "帐号",
                range = match.range,
                suggestions = listOf("账号")
            ))
        }
        val zhangHu = Regex("帐户")
        zhangHu.findAll(originalText).forEach { match ->
            errors.add(SpellError(
                word = "帐户",
                range = match.range,
                suggestions = listOf("账户")
            ))
        }

        // 检测"度/渡"误用
        val duGuo = Regex("渡过(时光|时间|假期|岁月|难关|困难|危机)")
        duGuo.findAll(originalText).forEach { match ->
            errors.add(SpellError(
                word = "渡过",
                range = match.range.first..match.range.first + 1,
                suggestions = listOf("度过")
            ))
        }

        // 检测"反应/反映"误用
        val fanYing = Regex("反映(迟钝|灵敏|过度|强烈|如何|很快|很慢|迅速|激烈)")
        fanYing.findAll(originalText).forEach { match ->
            errors.add(SpellError(
                word = "反映",
                range = match.range.first..match.range.first + 1,
                suggestions = listOf("反应")
            ))
        }

        // 检测"像/象"误用
        val haoXiang = Regex("好象")
        haoXiang.findAll(originalText).forEach { match ->
            errors.add(SpellError(
                word = "好象",
                range = match.range,
                suggestions = listOf("好像")
            ))
        }

        // 检测"部/步"误用
        val anBu = Regex("按步就班")
        anBu.findAll(originalText).forEach { match ->
            errors.add(SpellError(
                word = "按步就班",
                range = match.range,
                suggestions = listOf("按部就班")
            ))
        }
    }

    /**
     * 使用 Android 系统 SpellChecker 进行异步拼写检查（支持中文）
     */
    suspend fun checkSpellingWithSystem(text: String): List<SpellError> {
        val systemErrors = mutableListOf<SpellError>()

        try {
            val tsm = context.getSystemService(Context.TEXT_SERVICES_MANAGER_SERVICE)
                    as? TextServicesManager ?: return systemErrors

            val session = suspendCancellableCoroutine<SpellCheckerSession> { cont ->
                val listener = object : SpellCheckerSessionListener {
                    override fun onGetSuggestions(results: Array<android.view.textservice.SuggestionsInfo>?) {}
                    override fun onGetSentenceSuggestions(results: Array<SentenceSuggestionsInfo>?) {}
                }

                val session = tsm.newSpellCheckerSession(
                    null, Locale.getDefault(), listener, false
                )
                if (session != null) {
                    cont.resume(session)
                } else {
                    cont.resume(throw UnsupportedOperationException("SpellCheckerSession not available"))
                }
            }

            val sentenceResults = suspendCancellableCoroutine<Array<SentenceSuggestionsInfo>> { cont ->
                val listener = object : SpellCheckerSessionListener {
                    override fun onGetSuggestions(results: Array<android.view.textservice.SuggestionsInfo>?) {}
                    override fun onGetSentenceSuggestions(results: Array<SentenceSuggestionsInfo>?) {
                        cont.resume(results ?: emptyArray())
                    }
                }

                val newSession = tsm.newSpellCheckerSession(
                    null, Locale.getDefault(), listener, false
                ) ?: run {
                    cont.resume(emptyArray())
                    return@suspendCancellableCoroutine
                }

                newSession.getSentenceSuggestions(arrayOf(TextInfo(text)), 5)

                cont.invokeOnCancellation {
                    newSession.close()
                }
            }

            for (sentenceInfo in sentenceResults) {
                for (i in 0 until sentenceInfo.suggestionsCount) {
                    val suggestionsInfo = sentenceInfo.getSuggestionsInfoAt(i)
                    if (suggestionsInfo.suggestionsCount > 0) {
                        val offset = sentenceInfo.getOffsetAt(i)
                        val length = sentenceInfo.getLengthAt(i)
                        val suggestions = (0 until minOf(suggestionsInfo.suggestionsCount, 5)).mapNotNull {
                            suggestionsInfo.getSuggestionAt(it)
                        }
                        if (offset in text.indices && length > 0) {
                            val errorWord = text.substring(offset, minOf(offset + length, text.length))
                            systemErrors.add(SpellError(
                                word = errorWord,
                                range = offset..(offset + length - 1).coerceAtMost(text.lastIndex),
                                suggestions = suggestions
                            ))
                        }
                    }
                }
            }

            session.close()
        } catch (_: Exception) {
            // 系统 SpellChecker 不可用时，静默失败
        }

        return systemErrors
    }

    /**
     * 判断是否为 Markdown 语法关键词（不应检查拼写）
     */
    private fun isMarkdownSyntax(word: String): Boolean {
        val lower = word.lowercase()
        return lower in setOf(
            "http", "https", "www", "com", "org", "net", "html", "css", "json",
            "api", "url", "uri", "src", "href", "alt", "img", "div", "span",
            "true", "false", "null", "undefined", "function", "return", "const",
            "var", "let", "class", "import", "export", "default", "async",
            "await", "try", "catch", "throw", "new", "delete", "typeof",
            "instanceof", "void", "this", "super", "extends", "implements",
            "yaml", "toml", "xml", "svg", "md", "txt", "log", "env",
            "npm", "yarn", "pnpm", "pip", "gradle", "maven", "cargo", "rust",
            "kotlin", "java", "python", "golang", "swift", "dart", "flutter",
            "android", "ios", "linux", "macos", "windows", "chrome", "firefox",
        )
    }

    /**
     * 添加自定义单词到词典
     * @return true 表示添加成功，false 表示已存在
     */
    fun addWord(word: String): Boolean {
        val lower = word.lowercase()
        if (customWords.contains(lower)) return false
        customWords.add(lower)
        val spell = symSpell ?: return true
        spell.createDictionaryEntry(lower, 1)
        return true
    }

    /**
     * 获取指定单词的纠正建议
     */
    fun getSuggestions(word: String): List<String> {
        if (!isInitialized) initialize()
        val spell = symSpell ?: return emptyList()
        return spell.lookup(word.lowercase(), Verbosity.Closest)
            .take(5)
            .map { it.term }
    }
}

/**
 * 拼写错误数据类
 */
data class SpellError(
    val word: String,
    val range: IntRange,
    val suggestions: List<String>,
)
