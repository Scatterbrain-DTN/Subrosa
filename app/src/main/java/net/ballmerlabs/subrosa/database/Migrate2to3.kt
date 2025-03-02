package net.ballmerlabs.subrosa.database
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.util.UUID

class Migrate2to3: Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE posts ADD COLUMN post_id TEXT")
        val posts = db.query("SELECT id FROM posts");
        db.execSQL("CREATE UNIQUE INDEX index_posts_post_id ON posts(post_id)")
        posts.moveToFirst()
        for(x in 0 until  posts.count) {
            val idx = posts.getColumnIndex("id")
            val postId = posts.getInt(idx)
            db.execSQL("UPDATE posts SET post_id = ? where id = ?", arrayOf(UUID.randomUUID(), postId))
            posts.moveToNext()
        }

    }
}