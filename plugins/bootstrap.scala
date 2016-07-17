import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.concurrent.ThreadLocalRandom
import java.util.function.BiConsumer

import io.luna.game.event.{Event, EventListener}
import io.luna.game.model.item.ItemContainer
import io.luna.game.model.mobile._
import io.luna.game.model.mobile.attr.AttributeValue
import io.luna.game.model.mobile.update.UpdateFlagHolder.UpdateFlag
import io.luna.game.model.{Entity, EntityType, Position, World}
import io.luna.game.plugin.PluginFailureException
import io.luna.game.task.Task
import io.luna.net.msg.out._

import scala.collection.JavaConversions._
import scala.reflect.ClassTag
import scala.util.Random

/** A bootstrapper acting as the "master dependency" for all other plugins. All of the complex, high level, 'dirty work' is
  * done in this plugin in order to ensure that other plugins can be written as idiomatically as possible.
  *
  * The interception of posted events can be handled through the '>>' (intercept) and '>>@' (intercept at/on) methods. '>>' for
  * generic events and '>>@' for events that override the 'matches' method in the Event class. The only difference is that
  * '>>@' takes a set of arguments that will matched against the events arguments.
  *
  * Please note that normal methods and fields must come before event interception.
  *
  * Also, because this plugin acts as a master dependency great caution needs to be taken when modifying its contents. Changing
  * and/or removing the wrong thing could result in breaking every single plugin.
  */

// context instances
@inline val plugins = ctx.getPlugins
@inline val world = ctx.getWorld
@inline val service = ctx.getService


// common constants
@inline val RIGHTS_PLAYER = PlayerRights.PLAYER
@inline val RIGHTS_MOD = PlayerRights.MODERATOR
@inline val RIGHTS_ADMIN = PlayerRights.ADMINISTRATOR
@inline val RIGHTS_DEV = PlayerRights.DEVELOPER

@inline val TYPE_PLAYER = EntityType.PLAYER
@inline val TYPE_NPC = EntityType.NPC
@inline val TYPE_OBJECT = EntityType.OBJECT
@inline val TYPE_ITEM = EntityType.ITEM


// logging, prefer lazy 'msg' evaluation
def log(msg: Any) = logger.info(String.valueOf(msg))
def logIf(cond: Boolean, msg: => Any) = if (cond) {log(msg)}


// preconditions and plugin failure, prefer lazy 'msg' evaluation
def fail(msg: Any = "execution failure") = throw new PluginFailureException(msg)
def failIf(cond: Boolean, msg: => Any = "cond == false") = if (cond) {fail(msg)}


// aliases for utilities
def rand = ThreadLocalRandom.current


// message handling
def scalaToJavaFunc[E <: Event](func: (E, Player) => Unit): BiConsumer[E, Player] =
  new BiConsumer[E, Player] {
    override def accept(evt: E, plr: Player) = func.apply(evt, plr)
  }

def >>@[T <: Event](args: Any*)
                   (func: (T, Player) => Unit)
                   (implicit tag: ClassTag[T]) = {

  def submit(newArgs: Seq[AnyRef]) =
    pipelines.addEventListener(tag.runtimeClass,
      new EventListener(scalaToJavaFunc(
        (msg: T, plr) => if (msg.matches(newArgs: _*)) {func(msg, plr)}
      )))

  submit(args.collect {
    case any: Any => any.asInstanceOf[AnyRef]
  })
}

def >>[T <: Event](func: (T, Player) => Unit)
                  (implicit tag: ClassTag[T]) =
  pipelines.addEventListener(tag.runtimeClass, new EventListener(scalaToJavaFunc(func)))


// misc. global methods
def async(func: => Unit) = service.submit(new Runnable {
  override def run() = {
    try {
      func
    } catch {case e: Exception => e.printStackTrace()}
  }
})
def using(resource: AutoCloseable)
         (func: AutoCloseable => Unit) = {
  try {
    func(resource)
  } finally {
    resource.close()
  }
}


/** All implicit (monkey patching) classes below are basically 'extending' Java classes by creating new functions for them. We
  * do this to ensure that all code coming from Java is as concise and idiomatic (Scala-like) as possible.
  *
  * Feel free to add on to the existing code, but beware:
  * - Removing code will (most likely) break existing plugins
  * - Functions that are too ambiguous may end up doing more harm
  *   than good (their purpose should be relatively obvious)
  */

implicit class PlayerImplicits(player: Player) {
  def address = player.getSession.getHostAddress
  def name = player.getUsername.capitalize
  def rights = player.getRights
  def bank = player.getBank
  def inventory = player.getInventory
  def equipment = player.getEquipment
  def sendMessage(message: String) = player.queue(new GameChatboxMessageWriter(message))
  def sendWidgetText(text: String, widget: Int) = player.queue(new WidgetTextMessageWriter(text, widget))
  def sendForceTab(id: Int) = player.queue(new ForceTabMessageWriter(id))
  def sendChatboxInterface(id: Int) = player.queue(new ChatboxInterfaceMessageWriter(id))
  def sendSkillUpdate(id: Int) = player.queue(new SkillUpdateMessageWriter(id))
  def sendMusic(id: Int) = player.queue(new MusicMessageWriter(id))
  def sendSound(id: Int, loops: Int = 0, delay: Int = 0) = player.queue(new SoundMessageWriter(id, loops, delay))
  def sendInterface(id: Int) = player.queue(new InterfaceMessageWriter(id))
  def sendState(id: Int, value: Int) = player.queue(new StateMessageWriter(id, value))
  def sendColor(id: Int, color: Int) = player.queue(new ColorChangeMessageWriter(id, color))
  def flag(updateFlag: UpdateFlag) = player.getUpdateFlags.flag(updateFlag)
}

implicit class MobileEntityImplicits(mob: MobileEntity) {
  def attr[T](key: String): T = {
    val attr: AttributeValue[T] = mob.getAttributes.get(key)
    attr.get
  }
  def attr[T](key: String, value: T) = {
    val attr: AttributeValue[T] = mob.getAttributes.get(key)
    attr.set(value)
  }
  def attrEquals(key: String, equals: Any) = equals == attr(key)
}

implicit class PlayerRightsImplicits(rights: PlayerRights) {
  // We use '@' to distinguish from the 'normal' operators, but they mean the same thing.
  def <=@(other: PlayerRights) = rights.equalOrLess(other)
  def >=@(other: PlayerRights) = rights.equalOrGreater(other)
  def >@(other: PlayerRights) = rights.greater(other)
  def <@(other: PlayerRights) = rights.less(other)
  def ==@(other: PlayerRights) = rights.equal(other)
}

implicit class EntityImplicits(entity: Entity) {
  def position = entity.getPosition
  def x = entity.getPosition.getX
  def y = entity.getPosition.getY
  def z = entity.getPosition.getZ
}

implicit class WorldImplicits(world: World) {
  def addNpc(id: Int, position: Position) = {
    val npc = new Npc(ctx, id, position)
    world.getNpcs.add(npc)
    npc
  }

  def schedule(delay: Int, instant: Boolean = false)
              (action: Task => Unit) = {
    world.schedule(new Task(instant, delay) {
      override protected def execute() = {
        action(this)
      }
    })
  }

  def scheduleOnce(delay: Int)(action: => Unit) = {
    schedule(delay) { it =>
      action
      it.cancel()
    }
  }

  def scheduleUntil(delay: Int, predicate: Boolean)(action: => Unit) = {
    schedule(delay) { it =>
      if (!predicate) {
        action
      } else {
        it.cancel()
      }
    }
  }

  def scheduleTimes(delay: Int, times: Int)(action: => Unit) = {
    var loops = 0
    schedule(delay) { it =>
      if (loops == times) {
        it.cancel()
      } else {
        action
        loops += 1
      }
    }
  }

  // NOTE: Be careful using this, these types of tasks will >never< stop running as long
  // as the server is online!
  def scheduleForever(delay: Int, instant: Boolean = false)
                     (action: => Unit) = {
    schedule(delay, instant) { it =>
      action
    }
  }

  def messageToAll(str: String) = world.getPlayers.foreach(_.sendMessage(str))
}

implicit class ArrayImplicits[T](array: Array[T]) {
  def shuffle = {
    var i = array.length - 1
    while (i > 0) {
      val index = rand.nextInt(i + 1)
      val a = array(index)
      array(index) = array(i)
      array(i) = a
      i -= 1
    }
    array
  }

  def randomElement = array((rand.nextDouble * array.length).toInt)
}

implicit class DateTimeFormatterImplicits(formatter: DateTimeFormatter) {
  def formatDate(string: String) = formatter.format(LocalDate.parse(string))
}

implicit class ItemContainerImplicits(items: ItemContainer) {
  def bulkOperation(func: => Unit) {
    items.setFiringEvents(false)
    try {
      func
    } finally {
      items.setFiringEvents(true)
    }
    items.fireBulkItemsUpdatedEvent()
  }
}

implicit class IndexedSeqImplicits[T](seq: IndexedSeq[T]) {
  def shuffle = Random.shuffle(seq)
  def randomElement = seq((rand.nextDouble * seq.length).toInt)
}