package io.techery.janet.sample.async

import com.google.gson.Gson
import io.techery.janet.AsyncActionService
import io.techery.janet.Janet
import io.techery.janet.async.actions.ConnectAsyncAction
import io.techery.janet.async.sample.action.RequestResponseAction
import io.techery.janet.async.sample.model.Body
import io.techery.janet.gson.GsonConverter
import io.techery.janet.helper.ActionStateSubscriber
import io.techery.janet.nkzawa.SocketIO

const private val API_URL = "http://localhost:3000"


fun main(args: Array<String>) {

    val janet = Janet
            .Builder()
            .addService(AsyncActionService(API_URL, SocketIO(), GsonConverter(Gson())))
            .build()

    val messagePipe = janet.createPipe(RequestResponseAction::class.java)

    var action = RequestResponseAction()
    action.body = Body()
    action.body.id = 1
    action.body.data = "test"

    janet.createPipe(ConnectAsyncAction::class.java)
            .createObservable(ConnectAsyncAction())
            .subscribe(ActionStateSubscriber<ConnectAsyncAction>()
                    .onSuccess {
                        messagePipe
                                .createObservable(action)
                                .subscribe(ActionStateSubscriber<RequestResponseAction>()
                                        .onSuccess({ println(it) })
                                )

                    }
                    .onFail { action, throwable -> throwable.printStackTrace() }
            )


    while (true) {
        Thread.sleep(100)
    }


}


