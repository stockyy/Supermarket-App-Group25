const express = require("express")
const app = express()
const path = require("path")

// set view engine
app.set("view engine", "ejs")
app.set("views", path.join(__dirname, "views"))

// middleware
app.use(express.static(path.join(__dirname, "public")))
app.use(express.urlencoded({ extended: true }))

// routes
app.get("/", (req, res) => {
  res.render("index")
})

app.get("/login", (req, res) => {
  res.render("login") // done
})

app.get("/register", (req, res) => {
  res.render("register") // done
})

app.get("/product", (req, res) => {
  res.render("product") // done
})

app.get("/product_detail", (req, res) => {
  res.render("product_detail") // done
})

app.get("/basket", (req, res) => {
  res.render("basket") // done
})

app.get("/checkout", (req, res) => {
  res.render("checkout")  // done
})

app.get("/order_history", (req, res) => {
  res.render("order_history") // 
})

app.get("/profile", (req, res) => {
  res.render("profile") // done
})


// start server
app.listen(3000, () => {
  console.log("Server running on http://localhost:3000")
})